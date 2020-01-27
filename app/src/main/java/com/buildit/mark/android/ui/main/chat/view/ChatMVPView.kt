package com.buildit.mark.android.ui.main.bills.view

import com.amazonaws.mobileconnectors.lex.interactionkit.Response
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation
import com.buildit.mark.android.ui.base.view.MVPView

/**
 * Created by harshit.laddha on 25/01/2020
 */
interface ChatMVPView : MVPView {
    fun toggleBtnMode(isTextEnabled: Boolean)
    fun toggleInputMode(isTextEnabled: Boolean)
    fun setLexContinuation(continuation: LexServiceContinuation?)
    fun handleLexResponse(response: Response?)
    fun clearTextInput()
    fun handlerUserResponse(response: String?)
}