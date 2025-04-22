package org.classapp.network

data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val career: String = "",
    val organization: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val city: String = "",
    val country: String = "",
    val facebook: String = "",
    val linkedin: String = "",
    val twitter: String = "",
    val instagram: String = "",
    val whatsapp: String = "",
    val telegram: String = "",
    val skype: String = "",
    val weChat: String = "",
    val line: String = "",
    val profileImage: String? = null,
    val visibility: String? = "",
) {
    companion object {
        fun fromMap(data: Map<String, Any>?, userId: String): UserProfile? {
            if (data == null) return null
            val contact = data["contact"] as? Map<*, *>
            val location = data["location"] as? Map<*, *>

            return UserProfile(
                userId = userId,
                fullName = data["fullName"] as? String ?: "",
                career = data["career"] as? String ?: "",
                organization = data["organization"] as? String ?: "",
                email = contact?.get("email") as? String ?: "",
                phone = contact?.get("phone") as? String ?: "",
                website = contact?.get("website") as? String ?: "",
                facebook = contact?.get("facebook") as? String ?: "",
                linkedin = contact?.get("linkedin") as? String ?: "",
                twitter = contact?.get("twitter") as? String ?: "",
                instagram = contact?.get("instagram") as? String ?: "",
                whatsapp = contact?.get("whatsapp") as? String ?: "",
                telegram = contact?.get("telegram") as? String ?: "",
                skype = contact?.get("skype") as? String ?: "",
                weChat = contact?.get("weChat") as? String ?: "",
                line = contact?.get("line") as? String ?: "",
                city = location?.get("city") as? String ?: "",
                country = location?.get("country") as? String ?: "",
                profileImage = data["profileImage"] as? String,
                visibility = data["visibility"] as? String ?: "Public"
            )
        }
    }
}
