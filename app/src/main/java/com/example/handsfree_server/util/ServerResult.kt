package com.example.handsfree_server.util

sealed class ServerResult <out T>{

    data class Success<out T >(val data: T) : ServerResult<T>()
    data class Error(val exception: ServerException) : ServerResult<Nothing>()


    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data = $data]"
            is Error -> "Error[exception= $exception]"
        }
    }

}
class ServerException(val displayMessage:String,  private val errorMessage:String,  private val responseCode:Int) :Exception(){


    override val message: String?
        get() = "status code: $responseCode  \n message: $errorMessage"
}