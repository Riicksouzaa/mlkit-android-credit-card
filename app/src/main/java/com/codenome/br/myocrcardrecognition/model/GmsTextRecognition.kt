package com.codenome.br.myocrcardrecognition.model

import android.graphics.Bitmap
import android.media.Image
import com.codenome.br.myocrcardrecognition.entity.RecognizedLine
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class GmsTextRecognition {

    private val analyser = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    fun processFrame(frame: Image, rotationDegress: Int): Task<List<RecognizedLine>> {
        val inputImage = InputImage.fromMediaImage(frame, rotationDegress)

        return analyser.process(inputImage).continueWith { task ->
            task.result.textBlocks.flatMap { it.lines }.map { line -> line.toRecognizedLine() }
        }
    }

    fun processFrame(bitmap: Bitmap, rotationDegress: Int): Task<List<RecognizedLine>> {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegress)

        return analyser.process(inputImage).continueWith { task ->
            task.result.textBlocks.flatMap { it.lines }.map { line -> line.toRecognizedLine() }
        }
    }

    private fun Text.Line.toRecognizedLine(): RecognizedLine =
        RecognizedLine(text)
}