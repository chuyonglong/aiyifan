package com.aiyifan.app.feature.mine

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aiyifan.app.databinding.FragmentMineBinding
import com.aiyifan.app.feature.auth.LoginActivity
import com.aiyifan.app.feature.collection.CollectionActivity
import com.aiyifan.app.feature.history.HistoryActivity
import com.aiyifan.app.feature.proxy.ProxySettingsActivity

class MineFragment : Fragment() {
    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener { startActivity(Intent(requireContext(), LoginActivity::class.java)) }
        binding.historyButton.setOnClickListener { startActivity(Intent(requireContext(), HistoryActivity::class.java)) }
        binding.collectionButton.setOnClickListener { startActivity(Intent(requireContext(), CollectionActivity::class.java)) }
        binding.proxySettingsButton.setOnClickListener {
            startActivity(Intent(requireContext(), ProxySettingsActivity::class.java))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
