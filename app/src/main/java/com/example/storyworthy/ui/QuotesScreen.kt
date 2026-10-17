package com.example.storyworthy.ui

import android.app.Application
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.storyworthy.Quote
import com.example.storyworthy.ui.theme.StoryworthyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import androidx.core.content.edit

// TODO: Make a hashmap, finding a way to track if the randomly pulled quote is within the preferred keywords and is not from a blacklisted author.
class QuotesViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<QuoteUiState>(QuoteUiState.Loading)
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    private val sharedPreferences = application.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    private val client = OkHttpClient()

    init {
        showRandomQuote()
    }

    fun showRandomQuote() {
        val library = getQuotesLibrary()
        if (library.isEmpty()) { // TODO: or if preferences change
            fetchQuotesFromServer()
        } else {
            if (_uiState.value is QuoteUiState.Error) {
                _uiState.value = QuoteUiState.Loading
            }
            viewModelScope.launch {
                _uiState.value = QuoteUiState.Success(library.random())
            }
        }
    }

    private fun fetchQuotesFromServer() {
        _uiState.value = QuoteUiState.Loading
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://zenquotes.io/api/quotes/")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val body = response.body.string()
                    val jsonArray = JSONArray(body)
                    val newQuotes = mutableListOf<Quote>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        newQuotes.add(Quote(
                            text = obj.getString("q"),
                            author = obj.getString("a")
                        ))
                    }

                    val existingLibrary = getQuotesLibrary().toMutableSet()
                    existingLibrary.addAll(newQuotes)
                    saveQuotesLibrary(existingLibrary.toList())

                    _uiState.value = QuoteUiState.Success(newQuotes.random())
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = QuoteUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun getQuotesLibrary(): List<Quote> {
        val jsonString = sharedPreferences.getString("quotes_library", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Quote>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(Quote(obj.getString("text"), obj.getString("author")))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveQuotesLibrary(library: List<Quote>) {
        val jsonArray = JSONArray()
        library.forEach { quote ->
            val obj = JSONObject()
            obj.put("text", quote.text)
            obj.put("author", quote.author)
            jsonArray.put(obj)
        }
        sharedPreferences.edit {
            putString("quotes_library", jsonArray.toString())
        }
    }
}

sealed class QuoteUiState {
    object Loading : QuoteUiState()
    data class Success(val quote: Quote) : QuoteUiState()
    data class Error(val message: String) : QuoteUiState()
}

@Composable
fun QuotesScreen(
    modifier: Modifier = Modifier,
    viewModel: QuotesViewModel = viewModel(),
    onAddToEntryClick: (quoteText: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuote = (uiState as? QuoteUiState.Success)?.quote

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Today's Quote",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            )
            val formattedDate = LocalDate.now()
            val fullFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            Text(
                text = formattedDate.format(fullFormatter),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Illustration Background + Card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Background Illustration (Rendered behind the Quote Card)
            Image(
                painter = painterResource(id = com.example.storyworthy.R.drawable.sw_icon_faded),
                contentDescription = null,
                modifier = Modifier
                    .size(320.dp)
                    .padding(8.dp)
            )

            // Quote Card (Semi-transparent so background image appears behind it)
            when (val state = uiState) {
                is QuoteUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is QuoteUiState.Success -> {
                    QuoteCard(quote = state.quote)
                }
                is QuoteUiState.Error -> {
                    Text("Error loading quote. Please refresh the page and try again.")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp)) // Spaced further from the quote card

        // Buttons
        Button(
            onClick = {
                currentQuote?.let { quote ->
                    val formattedQuote = "\"${quote.text}\" — ${quote.author}"
                    onAddToEntryClick(formattedQuote)
                }
            },
            enabled = currentQuote != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Add to Today's Entry",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp)) // Spaced closer to each other

        TextButton(
            onClick = { viewModel.showRandomQuote() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "Regenerate Quote",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.background,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuoteCard(quote: Quote) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = quote.text,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                ),
                textAlign = TextAlign.Center,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "- ${quote.author} -",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.DarkGray
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuotesScreenPreview() {
    StoryworthyTheme {
        QuotesScreen()
    }
}
