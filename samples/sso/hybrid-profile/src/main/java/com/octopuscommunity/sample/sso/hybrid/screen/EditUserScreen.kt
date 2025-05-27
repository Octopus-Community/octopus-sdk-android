package com.octopuscommunity.sample.sso.hybrid.screen

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.octopuscommunity.sample.sso.hybrid.data.model.User
import com.octopuscommunity.sdk.ui.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    user: User,
    onSave: (User) -> Unit,
    onBack: () -> Unit
) {
    var editedUser by remember(user) { mutableStateOf(user) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Edit your profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            EditUserContent(
                modifier = Modifier.fillMaxWidth(),
                user = editedUser,
                onUserChanged = { editedUser = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(editedUser) }
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun EditUserContent(
    user: User,
    onUserChanged: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            value = user.nickname ?: "",
            label = { Text("Nickname") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false
            ),
            singleLine = true,
            onValueChange = { onUserChanged(user.copy(nickname = it.ifEmpty { null })) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Avatar")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = user.avatar == null,
                onClick = { onUserChanged(user.copy(avatar = null)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Text("None")
            }
            SegmentedButton(
                selected = user.avatar?.source == User.Avatar.Source.REMOTE,
                onClick = {
                    onUserChanged(
                        user.copy(
                            avatar = User.Avatar(
                                source = User.Avatar.Source.REMOTE,
                                data = "https://play-lh.googleusercontent.com/H6umM_74J78u2KrdnnnDwwLctekjZg5kghkx6LSSeQvt5plFI3E98bjWaLZm8lds0ixg=w480-h960-rw"
                            )
                        )
                    )
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Text("Url")
            }
            SegmentedButton(
                selected = user.avatar?.source == User.Avatar.Source.LOCAL,
                onClick = {
                    onUserChanged(
                        user.copy(
                            avatar = User.Avatar(
                                source = User.Avatar.Source.LOCAL,
                                data = Uri.Builder()
                                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                                    .authority(context.packageName)
                                    .path(R.drawable.ic_bloc_user.toString())
                                    .build()
                            )
                        )
                    )
                },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Text("Local Uri")
            }
        }

        if (user.avatar != null) {
            AsyncImage(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                model = user.avatar.data,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Information about the age")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = user.ageInformation == null,
                onClick = { onUserChanged(user.copy(ageInformation = null)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Text("Not checked")
            }
            SegmentedButton(
                selected = user.ageInformation == User.AgeInformation.LEGAL_AGE_REACHED,
                onClick = {
                    onUserChanged(
                        user.copy(
                            ageInformation = User.AgeInformation.LEGAL_AGE_REACHED
                        )
                    )
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Text(">= 16")
            }
            SegmentedButton(
                selected = user.ageInformation == User.AgeInformation.UNDERAGE,
                onClick = {
                    onUserChanged(
                        user.copy(
                            ageInformation = User.AgeInformation.UNDERAGE
                        )
                    )
                },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Text("< 16")
            }
        }
    }
}


@Preview
@Composable
private fun EditUserScreenPreview() {
    EditUserScreen(
        user = User(),
        onSave = {},
        onBack = {}
    )
}