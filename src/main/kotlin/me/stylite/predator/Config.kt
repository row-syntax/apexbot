package me.stylite.predator

import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

object Config {
    private var conf: Properties = Properties()

    init {
        try {
            conf = Properties().apply { load(FileInputStream("config.properties")) }
        } catch (e: FileNotFoundException) {
            conf.setProperty("prefix", System.getenv()["PREFIX"])
            conf.setProperty("token", System.getenv()["TOKEN"])
            conf.setProperty("apiKey", System.getenv()["APIKEY"])
//            conf.setProperty("dblkey", System.getenv()["DBLKEY"])
        }
    }

    operator fun get(key: String) = conf.getProperty(key)?.takeIf { it.isNotEmpty() }
        ?: throw IllegalStateException("$key is missing or was empty!")

    val prefix = this["prefix"].split(", ")
    val token = this["token"]
    val apiKey = this["apiKey"]
    //val dblKey = this["dblkey"]
}
