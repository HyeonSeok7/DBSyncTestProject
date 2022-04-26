package com.example.localsyncsampleproject

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface

class Utils {

    companion object {

        fun getExifData(context: Context, uri: Uri): Boolean {
            val columns = arrayOf(
                MediaStore.Images.Media.DATA,
            )
            var sLat: String? = null
            var sLon: String? = null
            val query = context.contentResolver.query(uri, columns, null, null, null)
            query?.use { cursor ->
                val urlColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToNext()
                val result = cursor.getString(urlColumn)
                val ef = ExifInterface(result)
                sLat = ef.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                sLon = ef.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)

//                Log.e("TAG_DATETIME", "${ef.getAttribute(ExifInterface.TAG_DATETIME)}")
//                Log.e("TAG_GPS_LATITUDE", "${ef.getAttribute(ExifInterface.TAG_GPS_LATITUDE)}")   // 위도
//                Log.e("TAG_GPS_LATITUDE_REF", "${ef.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)}")
//                Log.e("TAG_GPS_LONGITUDE", "${ef.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)}") // 경도
//                Log.e("TAG_GPS_LONGITUDE_REF", "${ef.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)}")
            }
            return !(sLat == null && sLon == null)
        }

        fun getExifGPSData(context: Context, uri: Uri): HashMap<String, String?> {
            val hashMap = HashMap<String, String?>()
            val columns = arrayOf(
                MediaStore.Images.Media.DATA,
            )
            var sLat: String? = null
            var sLon: String? = null
            val query = context.contentResolver.query(uri, columns, null, null, null)
            query?.use { cursor ->
                val urlColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToNext()
                val result = cursor.getString(urlColumn)
                //syncMedia에 바로 넣기
                val ef = ExifInterface(result)
                sLat = ef.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                sLon = ef.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)



                hashMap.put("lat", if (sLat.isNullOrEmpty()) null else sLat) // 위도
                hashMap.put("lng", if (sLon.isNullOrEmpty()) null else sLon) //경도
            }
            return hashMap
        }

        fun getFilePath(context: Context, uri: Uri): String {
            var filePath: String? = null
            val columns = arrayOf(
                MediaStore.Images.Media.DATA,
            )
            val query = context.contentResolver.query(uri, columns, null, null, null)
            query?.use { cursor ->
                val urlColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                cursor.moveToNext()
                val result = cursor.getString(urlColumn)
                filePath = result
            }
            return filePath!!
        }

    }

}