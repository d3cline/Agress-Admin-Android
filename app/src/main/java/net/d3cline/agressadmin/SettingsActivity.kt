// SettingsActivity.kt
package net.d3cline.agressadmin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var settingsManager: SettingsManager
    private lateinit var baseUrlEditText: EditText
    private lateinit var apiKeyEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        setContentView(R.layout.activity_settings)

        baseUrlEditText = findViewById(R.id.baseUrlEditText)
        apiKeyEditText = findViewById(R.id.apiKeyEditText)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Set current values
        baseUrlEditText.setText(settingsManager.baseUrl)
        apiKeyEditText.setText(settingsManager.apiKey)

        saveButton.setOnClickListener {
            // Save settings
            settingsManager.baseUrl = baseUrlEditText.text.toString()
            settingsManager.apiKey = apiKeyEditText.text.toString()
            finish()
        }

        cancelButton.setOnClickListener {
            // Discard changes
            finish()
        }
    }
}
