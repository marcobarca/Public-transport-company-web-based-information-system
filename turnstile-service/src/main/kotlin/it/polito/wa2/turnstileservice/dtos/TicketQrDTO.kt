package it.polito.wa2.turnstileservice.dtos

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.ongres.scram.common.bouncycastle.base64.Base64
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

data class TicketQrDTO(
    @JsonProperty("") var qrCode: String? = ""
){
    fun decodeQRCode(): String?{
        return try {
            val image: BufferedImage = ImageIO.read(ByteArrayInputStream(Base64.decode(this.qrCode)))
            val bufferedImageLuminanceSource = BufferedImageLuminanceSource((image))
            val binaryBitmap = BinaryBitmap(HybridBinarizer(bufferedImageLuminanceSource))
            val hintMap = kotlin.collections.HashMap<DecodeHintType, Boolean>()
            hintMap[DecodeHintType.PURE_BARCODE] = true
            val qrCodeReader = QRCodeReader()
            qrCodeReader.decode(binaryBitmap, hintMap).text
        }catch(ex: Exception){
            println(ex.localizedMessage)
            null
        }
    }

    //TODO: this must be deleted, only for debugging purpose
    fun generateQRCode():String?{
        return try {
            val qrCodeWriter = QRCodeWriter()
            val bitMatrix = qrCodeWriter.encode(qrCode, BarcodeFormat.QR_CODE, 256, 256)
            val byteArrayOutputStream = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", byteArrayOutputStream)
            Base64.toBase64String(byteArrayOutputStream.toByteArray())
        } catch (ex: Exception) {
            println(ex.localizedMessage)
            null
        }

    }
}
