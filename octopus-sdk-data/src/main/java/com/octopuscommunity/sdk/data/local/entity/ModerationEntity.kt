package com.octopuscommunity.sdk.data.local.entity

import kotlinx.serialization.Serializable

class ModerationEntity {
    @Serializable
    data class ReasonEntity(val code: Code?, val message: String? = null) {
        enum class Code {
            HTE, HRS, GV, AGG, VLC, SXC, NUD, SOLI, AEX, CEX, SSH, ILL, HEX, TER, HC, SPM, IMP,
            MIS, PII, UP, CIP, OTH
        }
    }
}