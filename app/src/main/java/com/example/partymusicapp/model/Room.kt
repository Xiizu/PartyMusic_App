package com.example.partymusicapp.model

data class Room (
    val id: Int,
    var label: String,
    var description: String,
    var code: String,
    var host_id: Int)
{
    override fun toString(): String {
        return "Room -> id : '$id', label : '$label', description : '$description', code : '$code', host_id : '$host_id"
    }
}