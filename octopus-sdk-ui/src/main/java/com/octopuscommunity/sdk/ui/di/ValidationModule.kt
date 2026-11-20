package com.octopuscommunity.sdk.ui.di

import com.octopuscommunity.sdk.ui.common.validation.ValidateEmail
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.validation.ValidateText
import org.koin.dsl.module

val octopusValidationModule =
    module {
        single<ValidateEmail> { ValidateEmail() }
        single<ValidateImage> { ValidateImage() }
        single<ValidateText> { ValidateText() }
    }
