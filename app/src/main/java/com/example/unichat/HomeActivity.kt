package com.example.unichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.unichat.ui.theme.UNIChatTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            UNIChatTheme {
                HomeScreen(
                    onLogout = {
                        auth.signOut()
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UNIChat") },
                actions = {
                    IconButton(onClick = { onLogout() }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { index -> selectedTab = index }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ChatsScreen()
                1 -> TranslateScreen()
                2 -> AddContactScreen()
                3 -> SettingsScreen()
            }
        }
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
            label = { Text("Chats") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Translate, contentDescription = "Translate") },
            label = { Text("Translate") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact") },
            label = { Text("Add Contact") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) }
        )
    }
}

@Composable
fun ChatsScreen() {
    val chatItems = remember { listOf<String>() } // Empty list initially

    if (chatItems.isEmpty()) {
        // No chats available
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "No Chats",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No chats yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Start a new chat or add a contact to begin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { /* Navigate to Add Contact or Start Chat */ }) {
                    Text("Start a New Chat")
                }
            }
        }
    } else {
        // Show chat items
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(chatItems.size) { index ->
                ChatItem(name = chatItems[index])
            }
        }
    }
}


@Composable
fun TranslateScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Translate Screen", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun SettingsScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Settings Screen", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun ChatList() {
    val chatItems = remember { listOf("Friend 1", "Friend 2", "Group Chat", "Friend 3") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(chatItems.size) { index ->
            ChatItem(name = chatItems[index])
        }
    }
}

@Composable
fun ChatItem(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Navigate to Chat Detail */ },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun AddContactScreen() {
    var contactName by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+1") } // Default country code
    var phoneNumber by remember { mutableStateOf("") }
    val contactsList = remember { mutableStateListOf<Pair<String, String>>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope() // CoroutineScope for launching snackbar
    val countryCodes = listOf("+1", "+91", "+44", "+61", "+81") // Add more country codes as needed

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Add Contact",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text("Contact Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dropdown for country code
                var expanded by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .weight(0.3f) // Explicit weight for the Box
                ) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = {},
                        label = { Text("Country Code") },
                        enabled = false,
                        modifier = Modifier
                            .clickable { expanded = true }
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        countryCodes.forEach { code ->
                            DropdownMenuItem(
                                text = { Text(code) },
                                onClick = {
                                    countryCode = code
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Phone number field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.weight(0.7f) // Explicit weight for the OutlinedTextField
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (contactName.isBlank() || phoneNumber.isBlank()) {
                        // Show Snackbar when fields are empty
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please fill in both fields.")
                        }
                    } else if (contactsList.any { it.second == "$countryCode $phoneNumber" }) {
                        // Show Snackbar when phone number already exists
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Contact with this phone number already exists.")
                        }
                    } else {
                        // Add new contact and show success Snackbar
                        contactsList.add(Pair(contactName, "$countryCode $phoneNumber"))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Contact added successfully!")
                        }
                        // Reset the text fields
                        contactName = ""
                        phoneNumber = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Contact")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your Contacts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (contactsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = "No contacts added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contactsList.size) { index ->
                        ContactCard(name = contactsList[index].first, email = contactsList[index].second)
                    }
                }
            }
        }
    }
}


@Composable
fun ContactCard(name: String, email: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Contact Icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
