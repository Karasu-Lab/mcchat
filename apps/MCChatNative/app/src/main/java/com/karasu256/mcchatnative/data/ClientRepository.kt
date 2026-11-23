package com.karasu256.mcchatnative.data

import com.karasu256.mcchatnative.model.User
import com.karasu256.mcchatnative.model.DrawerUser
import com.karasu256.mcchatnative.model.UserId

object ClientRepository {
    val currentUser = User(UserId("player"), "Player")

    private val user = DrawerUser(name = currentUser.name, subtitle = "ローカルユーザー")

    suspend fun loadUser(): DrawerUser = user
}
