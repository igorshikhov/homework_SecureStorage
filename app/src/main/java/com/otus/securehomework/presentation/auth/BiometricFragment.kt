package com.otus.securehomework.presentation.auth

import android.os.Bundle
import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.otus.securehomework.R
import com.otus.securehomework.data.Response
import com.otus.securehomework.databinding.FragmentBiomericBinding
import com.otus.securehomework.presentation.enable
import com.otus.securehomework.presentation.handleApiError
import com.otus.securehomework.presentation.home.HomeActivity
import com.otus.securehomework.presentation.startNewActivity
import com.otus.securehomework.presentation.visible
import com.otus.securehomework.crypto.biometric.BioModule
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BiometricFragment : Fragment(R.layout.fragment_biomeric) {
    @Inject
    lateinit var biometric : BioModule
    private lateinit var binding: FragmentBiomericBinding
    private val viewModel by viewModels<AuthViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentBiomericBinding.bind(view)
        binding.progressbar.visible(false)
        binding.buttonRegister.enable(false)
        binding.editTextPassword.addTextChangedListener {
            val email = binding.editTextEmailAddress.text.toString().trim()
            binding.buttonRegister.enable(email.isNotEmpty() && it.toString().isNotEmpty())
        }
        binding.buttonRegister.setOnClickListener {
            login()
        }
        binding.cancelRegistration.setOnClickListener {
            findNavController().navigate(R.id.action_biometricFragment_to_loginFragment)
        }
        viewModel.loginResponse.observe(viewLifecycleOwner) {
            binding.progressbar.visible(it is Response.Loading)
            when (it) {
                is Response.Success -> {
                    val userConnectionData = it.value.user
                    val json = Gson().toJson(userConnectionData)
                    biometric.registerUserBiometrics(
                        context = requireActivity(),
                        onSuccess = { requireActivity().startNewActivity(HomeActivity::class.java) },
                        userData = json
                    )

                    lifecycleScope.launch {
                        viewModel.saveAccessTokens(
                            it.value.user.access_token!!,
                            it.value.user.refresh_token!!
                        )
                    }
                }
                is Response.Failure -> handleApiError(it) { login() }
                Response.Loading -> Unit
            }
        }
    }

    private fun login() {
        val email = binding.editTextEmailAddress.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()
        viewModel.login(email, password)
    }
}
