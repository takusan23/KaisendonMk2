package io.github.takusan23.kaisendonmk2.MisskeyDataClass

import java.io.Serializable

// Misskeyのリアクションのでーたくらす
data class MisskeyReactionData(val reaction: String, var reactionCount: Int): Serializable