package me.stylite.predator.utils

import me.stylite.predator.Config
import redis.clients.jedis.Jedis

object Redis {
    private fun init(): Jedis {
        val redis = Jedis(Config.redisHost, Config.redisPort.toInt())
        redis.auth(Config.redisPass)
        return redis
    }

    fun getValByKey(key: String): String {
        val redis = init()
        val value = redis.get(key) ?: ""
        redis.close()
        return value
    }

    fun setValByKey(key: String, value: String) {
        val redis = init()
        redis.set(key, value)
        redis.close()
    }

}
