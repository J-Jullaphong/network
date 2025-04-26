package org.classapp.network

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val signUpButton = findViewById<Button>(R.id.signUpButton)
        val loginLink = findViewById<TextView>(R.id.loginLink)

        setEditTextIcon(emailEditText, FontAwesome.Icon.faw_envelope)
        setEditTextIcon(passwordEditText, FontAwesome.Icon.faw_lock)
        setEditTextIcon(confirmPasswordEditText, FontAwesome.Icon.faw_lock)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    registerUser(email, password)
                } else {
                    Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        loginLink.setOnClickListener {
            val intent = Intent(this, AuthenticationActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                    navigateToProfileSetup()
                } else {
                    Toast.makeText(
                        this,
                        "Sign Up Failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("SignUpActivity", "Error: ${task.exception}")
                }
            }
    }

    private fun navigateToProfileSetup() {
        val intent = Intent(this, ProfileSetupActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setEditTextIcon(editText: EditText, icon: IIcon) {
        val iconDrawable = IconicsDrawable(this, icon).apply {
            sizeDp = 20
            colorInt = resources.getColor(android.R.color.darker_gray, theme)
        }
        editText.setCompoundDrawablesWithIntrinsicBounds(iconDrawable, null, null, null)
        editText.compoundDrawablePadding = 16
    }
}
