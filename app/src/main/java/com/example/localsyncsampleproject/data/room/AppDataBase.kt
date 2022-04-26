package com.example.localsyncsampleproject.data.room

import android.app.Application
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.localsyncsampleproject.data.room.dao.MediaDao
import com.example.localsyncsampleproject.data.room.entity.Media

// 공식 문서에서는 데이터베이스 객체를 인서튼스 할 때 싱글톤으로 구현하기를 권장하고 있다.
// 일단 여러 인스턴스에 엑세스를 꼭 해야 하는 일이 거의 없고, 객체 생성에 비용이 많이 들기 때문이다.
@Database(entities = [Media::class], version = 1)
abstract class AppDataBase :RoomDatabase() {

    abstract fun mediaDao(): MediaDao

    companion object {
        private var INSTANCE: AppDataBase? = null

        @Synchronized
        fun create(application: Application): AppDataBase { // 단순히 생성만 하게 만들었지만 데이터가 없는경우 초기 셋팅을 진행할 수 있다. AAC Room에서는 간혹 에러가 난다?
            return INSTANCE ?: synchronized(this) { // 데이터베이스의 기존 인스턴스(이미 있는 경우)를 반환하거나 필요하다면 처음으로 데이터 베이스를 만든다.
                val instance = Room.databaseBuilder(
                    application,
                    AppDataBase::class.java,
                    "media-database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

}