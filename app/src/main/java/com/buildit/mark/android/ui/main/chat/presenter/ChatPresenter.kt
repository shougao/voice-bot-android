package com.buildit.mark.android.ui.main.bills.presenter

import android.content.Context
import android.util.Log
import com.amazonaws.mobileconnectors.lex.interactionkit.InteractionClient
import com.amazonaws.mobileconnectors.lex.interactionkit.Response
import com.amazonaws.mobileconnectors.lex.interactionkit.continuations.LexServiceContinuation
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView
import com.buildit.mark.android.data.database.repository.bills.BillsResponse
import com.buildit.mark.android.ui.base.presenter.BasePresenter
import com.buildit.mark.android.ui.main.bills.interactor.ChatMVPInteractor
import com.buildit.mark.android.ui.main.bills.view.ChatMVPView
import com.buildit.mark.android.util.FileUtils
import com.buildit.mark.android.util.SchedulerProvider
import com.google.gson.GsonBuilder
import com.google.gson.internal.`$Gson$Types`
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import javax.inject.Inject

/**
 * Created by harshit.laddha on 25/01/2020
 */
class ChatPresenter<V : ChatMVPView, I : ChatMVPInteractor> @Inject constructor(
        interactor: I, schedulerProvider: SchedulerProvider,
        compositeDisposable: CompositeDisposable) :
        BasePresenter<V, I>(interactor = interactor, schedulerProvider = schedulerProvider,
                compositeDisposable = compositeDisposable), ChatMVPPresenter<V, I> {

    private var previousResponse: Response? = null
    private lateinit var voiceBtn: InteractiveVoiceView
    private lateinit var lexInteractionClient: InteractionClient

    private val TAG = "chatPresenter"

    override fun updateInteractionClient(lexInteractionClient: InteractionClient) {
        this.lexInteractionClient = lexInteractionClient
    }

    override fun submitTextMessage(inputTextMessage: String,
                                   lexServiceContinuation: LexServiceContinuation?,
                                   hideMessageFromWindow: Boolean) {
        lexInteractionClient.cancel()
        if (lexServiceContinuation != null) {
            lexServiceContinuation.continueWithTextInForAudioOut(inputTextMessage)
        } else {
            lexInteractionClient.textInForAudioOut(inputTextMessage,
                    null)
        }
        getView()?. let {
            if (!hideMessageFromWindow) it.handlerUserResponse(inputTextMessage)
            it.showChatProgress(true)
        }
    }

    override fun onViewPrepared(context: Context, voiceBtn: InteractiveVoiceView,
                                lexInteractionClient: InteractionClient) {
        this.voiceBtn = voiceBtn
        this.voiceBtn.setInteractiveVoiceListener(this)
        this.lexInteractionClient = lexInteractionClient
        this.lexInteractionClient.setAudioPlaybackListener(this)
        this.lexInteractionClient.setInteractionListener(this)
        this.lexInteractionClient.setMicrophoneListener(this)
        loadBillData(context)
        getView()?. let {
            submitTextMessage(it.getInitIntent(), null, true)
        }
    }

    private fun loadBillData(context: Context) {
        val bills: String = FileUtils.loadJSONFromAsset(context, "bills.json")
        val builder = GsonBuilder().excludeFieldsWithoutExposeAnnotation()
        val gson = builder.create()
        val type = `$Gson$Types`.newParameterizedTypeWithOwner(
                null, BillsResponse::class.java)
        val response = gson.fromJson<BillsResponse>(bills, type)
        getView()?.let {
            it.setBillList(response)
        }
    }

    override fun dialogReadyForFulfillment(slots: MutableMap<String, String>?, intent: String?) {
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()))
        getView()?.showChatProgress(false)
    }

    override fun onResponse(response: Response?) {
        Log.d(TAG, "Bot response: " + response?.textResponse)
        Log.d(TAG, "Transcript: " + response?.inputTranscript)
        getView()?.let {
            response?. let {it2 ->
                if (it2.inputTranscript != null && it2.inputTranscript.isNotEmpty()) {
                    if (!it2.dialogState.equals("elicitintent", true)) {
                        it.handlerUserResponse(it2.inputTranscript)
                        if (previousResponse == null) it.handleLexResponse(it2)
                        else {
                            previousResponse?. let {it3 ->
                                if (it3.intentName != it2.intentName
                                        || it3.slotToIllicit != it2.slotToIllicit) {
                                    it.handleLexResponse(it2)
                                } else {
                                    lexInteractionClient.cancel()
                                    it.toggleBtnMode(true)
                                    it.toggleInputMode(true)
                                }
                            }
                        }
                    }
                } else {
                    it.toggleBtnMode(true)
                    it.toggleInputMode(true)
                    lexInteractionClient.cancel()
                }
                it.showChatProgress(false)
            }
        }
//        previousResponse?. let {
//            response?. let {it1 ->
//                getView()?. let {it2 ->
//                    if (it.intentName == it1.intentName &&
//                            it.slotToIllicit == it1.slotToIllicit) {
//                        it2.toggleAudioPlayback(false)
//                    } else {
//                        it2.toggleAudioPlayback(true)
//                    }
//                }
//            }
//        }
        previousResponse = response
    }

    override fun onError(responseText: String?, e: Exception?) {
        Log.e(TAG, "Error: $responseText", e)
        getView()?. let {
            it.showChatProgress(false)
            it.toggleBtnMode(true)
            it.toggleInputMode(true)
        }
    }

    override fun promptUserToRespond(response: Response?, continuation: LexServiceContinuation?) {
        Log.e(TAG, "Response: ${response?.textResponse}")
        getView()?.let {
            it.clearTextInput()
            it.setLexContinuation(continuation)
            it.handleLexResponse(response)
            it.showChatProgress(false)
        }
    }

    override fun onReadyForFulfillment(response: Response?) {
        Log.e(TAG, "onReadyForFulfillment:")
        getView()?.showChatProgress(false)
    }

    override fun onInteractionError(response: Response?, e: java.lang.Exception?) {
        Log.e(TAG, "onInteractionError:", e)
        getView()?.showChatProgress(false)
    }

    override fun onAudioPlaybackError(e: java.lang.Exception?) {
        Log.e(TAG, "onAudioPlaybackError:", e)
    }

    override fun onAudioPlayBackCompleted() {
        Log.e(TAG, "onAudioPlayBackCompleted")
    }

    override fun onAudioPlaybackStarted() {
        Log.e(TAG, "onAudioPlaybackStarted")
    }

    override fun readyForRecording() {
        Log.e(TAG, "readyForRecording")
    }

    override fun onRecordingEnd() {
        Log.e(TAG, "onRecordingEnd")
    }

    override fun startedRecording() {
        Log.e(TAG, "startedRecording")
    }

    override fun onMicrophoneError(e: java.lang.Exception?) {
        Log.e(TAG, "onMicrophoneError", e)
    }

    override fun onSoundLevelChanged(soundLevel: Double) {
        Log.e(TAG, "onSoundLevelChanged")
    }

}