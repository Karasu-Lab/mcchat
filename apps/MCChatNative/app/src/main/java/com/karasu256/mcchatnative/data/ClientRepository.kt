package com.karasu256.mcchatnative.data

import com.karasu256.mcchatnative.model.ChatUser
import com.karasu256.mcchatnative.model.Client
import com.karasu256.mcchatnative.model.DrawerUser
import com.karasu256.mcchatnative.model.UserId

object ClientRepository {
    private val clients = listOf(
        Client("survival-main", "Survival Main", "play.example.com:25565", isOnline = true),
        Client("creative-build", "Creative Build", "192.168.0.12:25565", isOnline = true),
        Client("modpack-test", "Modpack Test", "localhost:25566", isOnline = false),
    )

    val currentUser = ChatUser(UserId("player"), "Player")

    private val user = DrawerUser(name = currentUser.name, subtitle = "ローカルユーザー")

    suspend fun loadClients(): List<Client> = clients

    fun findClient(id: String): Client? = clients.firstOrNull { it.id == id }

    suspend fun loadUser(): DrawerUser = user
}
