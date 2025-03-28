package org.classapp.network

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client

object AWSConfig {
    private const val ACCESS_KEY = BuildConfig.AWS_ACCESS_KEY
    private const val SECRET_KEY = BuildConfig.AWS_SECRET_KEY
    private const val REGION = BuildConfig.AWS_REGION

    private val credentials = BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)

    val s3Client: AmazonS3Client = AmazonS3Client(credentials).apply {
        setRegion(com.amazonaws.regions.Region.getRegion(Regions.fromName(REGION)))
    }
}
