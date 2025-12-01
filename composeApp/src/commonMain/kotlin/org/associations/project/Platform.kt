package org.associations.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform