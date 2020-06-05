package io.github.takusan23.kaisendonmk2.BottomFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.takusan23.kaisendonmk2.MisskeyAPI.MisskeyReactionAPI
import io.github.takusan23.kaisendonmk2.MisskeyDataClass.MisskeyNoteData
import io.github.takusan23.kaisendonmk2.R
import kotlinx.android.synthetic.main.bottom_fragment_misskey_reaction.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * リアクションする時に使うBottomSheet
 * */
class MisskeyReactionBottomSheet(var noteData: MisskeyNoteData) : BottomSheetDialogFragment() {

    val misskeyReactionAPI = MisskeyReactionAPI(noteData.instanceToken)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottom_fragment_misskey_reaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // IMEから絵文字選んでPOST
        bottom_fragment_misskey_reaction_post.setOnClickListener {
            val input = bottom_fragment_misskey_reaction_input.text.toString()
            postReaction(input)
        }
        // 絵文字ボタン
        bottom_fragment_misskey_reaction_good.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_good.text.toString())
        }
        bottom_fragment_misskey_reaction_heart.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_heart.text.toString())
        }
        bottom_fragment_misskey_reaction_smiling.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_smiling.text.toString())
        }
        bottom_fragment_misskey_reaction_thinking.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_thinking.text.toString())
        }
        bottom_fragment_misskey_reaction_open.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_open.text.toString())
        }
        bottom_fragment_misskey_reaction_party.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_party.text.toString())
        }
        bottom_fragment_misskey_reaction_anger.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_anger.text.toString())
        }
        bottom_fragment_misskey_reaction_sad.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_sad.text.toString())
        }
        bottom_fragment_misskey_reaction_halo.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_halo.text.toString())
        }
        bottom_fragment_misskey_reaction_custard.setOnClickListener {
            postReaction(bottom_fragment_misskey_reaction_custard.text.toString())
        }
    }

    // リアクションPOST
    fun postReaction(reaction: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val response = withContext(Dispatchers.IO) {
                misskeyReactionAPI.reaction(noteData.noteId, reaction).await()
            }
            if (response.isSuccessful) {
                Toast.makeText(context, "${context?.getString(R.string.reaction_ok)}：${reaction}", Toast.LENGTH_SHORT).show()
                // 更新
                noteData.reaction.forEach { reactionData ->
                    if (reactionData.reaction == reaction) {
                        reactionData.reactionCount++
                    }
                }
                dismiss()
            } else {
                Toast.makeText(context, "${context?.getString(R.string.error)}：${response.code}", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
    }

}