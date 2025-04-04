package com.octopuscommunity.sample.sso.client.screens

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
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
import com.octopuscommunity.sample.sso.client.data.AppUser
import com.octopuscommunity.sdk.domain.model.ClientUser.Profile.AgeInformation
import com.octopuscommunity.sdk.domain.model.Image
import com.octopuscommunity.sdk.domain.model.ProfileField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SSOEditUserScreen(
    initialUser: AppUser,
    appManagedFields: Set<ProfileField>,
    displayAge: Boolean,
    onSave: (AppUser) -> Unit,
    onBack: () -> Unit
) {
    var appUser by remember(initialUser) { mutableStateOf(initialUser) }

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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            EditUserContent(
                modifier = Modifier.fillMaxWidth(),
                appUser = appUser,
                visibleFields = appManagedFields,
                displayAge = displayAge,
                onAppUserChanged = { appUser = it }
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSave(appUser) }
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun EditUserContent(
    appUser: AppUser,
    visibleFields: Set<ProfileField>,
    displayAge: Boolean,
    onAppUserChanged: (AppUser) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        if (ProfileField.NICKNAME in visibleFields) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                value = appUser.nickname ?: "",
                label = { Text("Nickname") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrectEnabled = false
                ),
                singleLine = true,
                onValueChange = { onAppUserChanged(appUser.copy(nickname = it.ifEmpty { null })) },
            )
        }

        if (ProfileField.BIO in visibleFields) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                value = appUser.bio ?: "",
                label = { Text("Bio") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                onValueChange = { onAppUserChanged(appUser.copy(bio = it.ifEmpty { null })) },
            )
        }

        if (ProfileField.AVATAR in visibleFields) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Avatar")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = appUser.avatar == null,
                    onClick = { onAppUserChanged(appUser.copy(avatar = null)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("None")
                }
                SegmentedButton(
                    selected = appUser.avatar is Image.Remote,
                    onClick = {
                        onAppUserChanged(
                            appUser.copy(
                                avatar = Image.Remote("https://play-lh.googleusercontent.com/H6umM_74J78u2KrdnnnDwwLctekjZg5kghkx6LSSeQvt5plFI3E98bjWaLZm8lds0ixg=w480-h960-rw")
                            )
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text("Url")
                }
                SegmentedButton(
                    selected = appUser.avatar is Image.Local,
                    onClick = {
                        onAppUserChanged(
                            appUser.copy(
                                avatar = Image.Local(
                                    Uri.Builder()
                                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                                        .authority(context.packageName)
                                        .path(com.octopuscommunity.sdk.ui.R.drawable.ic_bloc_user.toString())
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

            if (appUser.avatar != null) {
                Spacer(modifier = Modifier.height(16.dp))

                AsyncImage(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    model = when (val avatar = appUser.avatar) {
                        is Image.Remote -> avatar.url
                        is Image.Local -> avatar.uri
                        else -> null
                    },
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                )
            }
        }

        if (displayAge) {
            Spacer(modifier = Modifier.height(16.dp))

            Text("Information about the age")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = appUser.ageInformation == null,
                    onClick = { onAppUserChanged(appUser.copy(ageInformation = null)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) {
                    Text("Not checked")
                }
                SegmentedButton(
                    selected = appUser.ageInformation == AgeInformation.LegalAgeReached,
                    onClick = {
                        onAppUserChanged(
                            appUser.copy(
                                ageInformation = AgeInformation.LegalAgeReached
                            )
                        )
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) {
                    Text(">= 16")
                }
                SegmentedButton(
                    selected = appUser.ageInformation == AgeInformation.Underage,
                    onClick = {
                        onAppUserChanged(
                            appUser.copy(
                                ageInformation = AgeInformation.Underage
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
}

@Preview
@Composable
private fun SSOEditUserScreenPreview() {
    SSOEditUserScreen(
        initialUser = AppUser(),
        appManagedFields = ProfileField.ALL,
        displayAge = true,
        onSave = {},
        onBack = {}
    )
}