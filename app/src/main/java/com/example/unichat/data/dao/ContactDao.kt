package com.example.unichat.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.unichat.data.entities.Contact

@Dao
interface ContactDao {
    @Insert
    suspend fun insertContact(contact: Contact)

    @Query("SELECT * FROM contacts")
    suspend fun getAllContacts(): List<Contact>
}
