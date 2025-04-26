package org.classapp.network

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.amazonaws.mobileconnectors.s3.transferutility.*
import com.amazonaws.services.s3.AmazonS3Client
import com.beastwall.localisation.Localisation
import com.beastwall.localisation.model.Country
import com.beastwall.localisation.model.State
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.simpleicons.SimpleIcons
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import java.io.*
import java.util.concurrent.Executors

class ProfileSetupActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var fullNameEditText: EditText
    private lateinit var careerEditText: EditText
    private lateinit var organizationEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var websiteEditText: EditText
    private lateinit var cityAutoComplete: AutoCompleteTextView
    private lateinit var countryAutoComplete: AutoCompleteTextView
    private lateinit var facebookEditText: EditText
    private lateinit var linkedinEditText: EditText
    private lateinit var twitterEditText: EditText
    private lateinit var instagramEditText: EditText
    private lateinit var whatsappEditText: EditText
    private lateinit var telegramEditText: EditText
    private lateinit var skypeEditText: EditText
    private lateinit var weChatEditText: EditText
    private lateinit var lineEditText: EditText
    private lateinit var saveProfileButton: Button
    private lateinit var cancelButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val s3Client: AmazonS3Client = AWSConfig.s3Client
    private var imageUri: Uri? = null
    private val BUCKET_NAME = BuildConfig.AWS_S3_BUCKET

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private var existingVisibility: String = "Public"

    private var existingProfileImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        profileImageView = findViewById(R.id.profileImageView)
        fullNameEditText = findViewById(R.id.fullNameEditText)
        careerEditText = findViewById(R.id.careerEditText)
        organizationEditText = findViewById(R.id.organizationEditText)
        phoneEditText = findViewById(R.id.phoneEditText)
        emailEditText = findViewById(R.id.emailEditText)
        websiteEditText = findViewById(R.id.websiteEditText)
        cityAutoComplete = findViewById(R.id.cityAutoComplete)
        countryAutoComplete = findViewById(R.id.countryAutoComplete)
        facebookEditText = findViewById(R.id.facebookEditText)
        linkedinEditText = findViewById(R.id.linkedinEditText)
        twitterEditText = findViewById(R.id.twitterEditText)
        instagramEditText = findViewById(R.id.instagramEditText)
        whatsappEditText = findViewById(R.id.whatsappEditText)
        telegramEditText = findViewById(R.id.telegramEditText)
        skypeEditText = findViewById(R.id.skypeEditText)
        weChatEditText = findViewById(R.id.weChatEditText)
        lineEditText = findViewById(R.id.lineEditText)
        saveProfileButton = findViewById(R.id.saveProfileButton)

        setEditTextIcon(fullNameEditText, FontAwesome.Icon.faw_user)
        setEditTextIcon(careerEditText, FontAwesome.Icon.faw_briefcase)
        setEditTextIcon(organizationEditText, FontAwesome.Icon.faw_building)
        setEditTextIcon(phoneEditText, FontAwesome.Icon.faw_phone)
        setEditTextIcon(emailEditText, FontAwesome.Icon.faw_envelope)
        setEditTextIcon(websiteEditText, FontAwesome.Icon.faw_globe)
        setEditTextIcon(countryAutoComplete, FontAwesome.Icon.faw_flag)
        setEditTextIcon(cityAutoComplete, FontAwesome.Icon.faw_map_marker_alt)

        setEditTextIcon(facebookEditText, SimpleIcons.Icon.sim_facebook)
        setEditTextIcon(linkedinEditText, SimpleIcons.Icon.sim_linkedin)
        setEditTextIcon(twitterEditText, SimpleIcons.Icon.sim_twitter)
        setEditTextIcon(instagramEditText, SimpleIcons.Icon.sim_instagram)
        setEditTextIcon(whatsappEditText, SimpleIcons.Icon.sim_whatsapp)
        setEditTextIcon(telegramEditText, SimpleIcons.Icon.sim_telegram)
        setEditTextIcon(skypeEditText, SimpleIcons.Icon.sim_skype)
        setEditTextIcon(weChatEditText, SimpleIcons.Icon.sim_wechat)
        setEditTextIcon(lineEditText, SimpleIcons.Icon.sim_line)

        profileImageView.setOnClickListener {
            selectImageFromGallery()
        }

        cancelButton = findViewById(R.id.cancelButton)

        cancelButton.setOnClickListener {
            finish()
        }

        saveProfileButton.setOnClickListener {
            saveUserProfile()
        }

        fetchUserProfile()
        loadCountryAndCityData()
    }

    private fun fetchUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("UserProfiles").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userProfile = document.data ?: return@addOnSuccessListener

                    fullNameEditText.setText(userProfile["fullName"] as? String)
                    careerEditText.setText(userProfile["career"] as? String)
                    organizationEditText.setText(userProfile["organization"] as? String)

                    val contact = userProfile["contact"] as? Map<String, String>
                    contact?.let {
                        emailEditText.setText(it["email"])
                        phoneEditText.setText(it["phone"])
                        websiteEditText.setText(it["website"])
                        facebookEditText.setText(it["facebook"])
                        linkedinEditText.setText(it["linkedin"])
                        twitterEditText.setText(it["twitter"])
                        instagramEditText.setText(it["instagram"])
                        whatsappEditText.setText(it["whatsapp"])
                        telegramEditText.setText(it["telegram"])
                        skypeEditText.setText(it["skype"])
                        weChatEditText.setText(it["weChat"])
                        lineEditText.setText(it["line"])
                    }

                    val location = userProfile["location"] as? Map<String, String>
                    location?.let {
                        cityAutoComplete.setText(it["city"])
                        countryAutoComplete.setText(it["country"])
                    }

                    val profileImageUrl = userProfile["profileImage"] as? String
                    profileImageUrl?.let {
                        existingProfileImageUrl = it
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(profileImageView)
                    }

                    existingVisibility = userProfile["visibility"] as? String ?: "Public"
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch profile: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun loadCountryAndCityData() {
        executor.execute {
            val countries = Localisation.getAllCountriesStatesAndCities()
            val countryNames = countries.map { it.name }

            handler.post {
                val countryAdapter =
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countryNames)
                countryAutoComplete.setAdapter(countryAdapter)

                countryAutoComplete.setOnItemClickListener { _, view, _, _ ->
                    val selectedCountryName = (view as TextView).text.toString()
                    val selectedCountry = countries.find { it.name == selectedCountryName }

                    if (selectedCountry != null) {
                        loadCitiesForCountry(selectedCountry)
                    }
                }

                val selectedCountryName = (countryAutoComplete as TextView).text.toString()
                val selectedCountry = countries.find { it.name == selectedCountryName }

                if (selectedCountry != null) {
                    loadCitiesForCountry(selectedCountry)
                }
            }
        }
    }

    private fun loadCitiesForCountry(selectedCountry: Country) {
        executor.execute {
            val cityNames = selectedCountry.getStates()
                .flatMap(State::getCities)
                .map { it.name }

            handler.post {
                val cityAdapter =
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cityNames)
                cityAutoComplete.setAdapter(cityAdapter)
            }
        }
    }

    private fun setEditTextIcon(editText: EditText, icon: IIcon) {
        val iconDrawable = IconicsDrawable(this, icon).apply {
            sizeDp = 20
            colorInt = resources.getColor(android.R.color.darker_gray, theme)
        }
        editText.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null)
        editText.compoundDrawablePadding = 16
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            profileImageView.setImageURI(imageUri)
        }
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        val contactDetails = mutableMapOf<String, String>()

        fun addIfNotEmpty(field: EditText, key: String) {
            val value = field.text.toString().trim()
            if (value.isNotEmpty()) contactDetails[key] = value
        }

        addIfNotEmpty(emailEditText, "email")
        addIfNotEmpty(phoneEditText, "phone")
        addIfNotEmpty(websiteEditText, "website")
        addIfNotEmpty(facebookEditText, "facebook")
        addIfNotEmpty(linkedinEditText, "linkedin")
        addIfNotEmpty(twitterEditText, "twitter")
        addIfNotEmpty(instagramEditText, "instagram")
        addIfNotEmpty(whatsappEditText, "whatsapp")
        addIfNotEmpty(telegramEditText, "telegram")
        addIfNotEmpty(skypeEditText, "skype")
        addIfNotEmpty(weChatEditText, "weChat")
        addIfNotEmpty(lineEditText, "line")

        val userProfile = mutableMapOf(
            "fullName" to fullNameEditText.text.toString(),
            "career" to careerEditText.text.toString(),
            "organization" to organizationEditText.text.toString(),
            "contact" to contactDetails,
            "location" to mapOf(
                "city" to cityAutoComplete.text.toString(),
                "country" to countryAutoComplete.text.toString()
            ),
            "visibility" to existingVisibility
        )

        val updatedUserProfile = HashMap(userProfile)

        if (imageUri != null) {
            uploadProfileImageToS3(userId, imageUri!!) { imageUrl ->
                updatedUserProfile["profileImage"] = imageUrl
                saveToFirestore(userId, updatedUserProfile)
            }
        } else {
            existingProfileImageUrl?.let {
                updatedUserProfile["profileImage"] = it
            }
            saveToFirestore(userId, updatedUserProfile)
        }
    }

    private fun uploadProfileImageToS3(userId: String, imageUri: Uri, onSuccess: (String) -> Unit) {
        val file = convertUriToFile(imageUri)
        val transferUtility = TransferUtility.builder()
            .defaultBucket(BUCKET_NAME)
            .s3Client(s3Client)
            .context(this)
            .build()

        val uploadObserver: TransferObserver = transferUtility.upload(
            "profile_images/$userId.jpg",
            file
        )

        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                if (state == TransferState.COMPLETED) {
                    val imageUrl =
                        "https://$BUCKET_NAME.s3.amazonaws.com/profile_images/$userId.jpg"
                    onSuccess(imageUrl)
                } else if (state == TransferState.FAILED) {
                    Toast.makeText(
                        this@ProfileSetupActivity,
                        "Image upload failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {}
            override fun onError(id: Int, ex: Exception?) {
                Toast.makeText(
                    this@ProfileSetupActivity,
                    "Upload error: ${ex?.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun convertUriToFile(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "upload_image.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun saveToFirestore(userId: String, userProfile: HashMap<String, Any>) {
        db.collection("UserProfiles").document(userId)
            .set(userProfile)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save profile: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
