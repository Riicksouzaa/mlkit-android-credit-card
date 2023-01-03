package com.codenome.br.myocrcardrecognition.model

import android.media.Image
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeRecognition {
    private val analyzer = BarcodeScanning.getClient()
    fun processFrame(frame: Image, rotationDegreesValue: Int): Task<MutableList<Barcode>> {
        val inputImage = InputImage.fromMediaImage(frame, rotationDegreesValue)
        return analyzer.process(inputImage).continueWith { task -> task.result }
    }
}