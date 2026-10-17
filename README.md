![Java](https://img.shields.io/badge/Made%20with-Java-007396?logo=java&logoColor=white)
![Kotlin](https://img.shields.io/badge/Made%20with-Kotlin-FF8800?logo=kotlin&logoColor=white)

> Note that this application is still in development. Several features are incomplete, and it has not
been rigorously tested for bugs and database security.

## Table of Contents

# Storyworthy

![Storywothy](app/src/main/res/drawable/storyworthy_ico.png)

An Android application made for diarists, from aspiring to advanced. Write entries daily and feel
its cognitive benefits.

Journaling on pen and paper can feel overwhelming for new users, who may not know where to start.
Storyworthy aims to address this hurdle by providing an assistant that asks thought-provoking
questions about the user's provided context, allowing them to: 
- Establish a starting point for their writing 
- Experience input options that are even more freeform than pen and paper.  

Current solutions on the market suffer from the following:
- **Inaccessibility**. New diarists may struggle to know where to start when staring at a blank page. 
Storyworthy addresses this by giving users an optional AI assistant to nudge them in a direction that
can get their creative writing flowing.

- **Privacy concerns**. Users could theoretically journal their thoughts in the popular LLMs, but 
there are reasonable worries about giving sensitive information to them. Storyworthy addresses this
by keeping searches independent of user accounts and prompts are generated with keywords and mood
only, preventing LLMs from obtaining a fingerprint. All entry data is saved secured in a Firebase
Realtime Database, where user data is not sold for profit.

- **Limited recollection/thought tracking**. Dedicated diarists can fill up several journals by
themselves, making it become ever-harder to locate certain entries or assess if and how the
journaling process has improved their mood. Storyworthy addresses this by having a sentiment and
semantic-based lookup of entries, allowing users to filter through entries far faster than they
could in a physical journal. There is also a favorite feature for users to pin poignant entries to
the top of their view.

---

## Getting Started

--- 

## Usage

### Home 
Once entering the app, you will be prompted to Sign In. If you have an account, log in with your
email and password credentials. Otherwise, you can create an account manually or create an account
with Google.

The home page highlights your entry history. You are able to change the view to a list or calendar
view. Either view can be searched by mood, sentiment, or date to lookup entries quickly.

### Quotes
Users are provided with a daily quote from a library of inspirational quotes that can be a source of
daily encouragement. If users do not like the displayed quote, they can regenerate the quote to find
one they like. These quotes can also be added to an entry.

### Add Entry
Users are prompted to select an appropriate mood for how they are feeling, and if they would like AI
to assist them in the journaling process. If 'no' is selected, users can proceed to freeform writing 
their entry. Otherwise, sentiments will 

## Acknowledgements
A big thanks goes out to Jiewan, Rohit, Shreyas, Raihan, and Edgar for helping this project come to
life.

### Checklist
- [] Assess working state of login/sign up 
- [] Build view entry page, have "page flip" functionality
- [] Build entry search functionality
- [] Finish settings screen
- [] Add sanity check functionality (confirm log out, etc.)
- [] Build stats page
- [] Update UI and icons to reflect their functions better
- [] Optimize app performance (rewrite functions as needed, save localStorage, etc.)
- [] Optimize code (refactor, write reusable functions, etc.)
- [] Add fun, informative animations