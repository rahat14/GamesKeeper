package com.foxhole.gameskeeper.ui.explore

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import com.foxhole.gameskeeper.adapter.GameAdapter
import com.foxhole.gameskeeper.adapter.GameLoadStateAdapter
import com.foxhole.gameskeeper.base.BaseFragment
import com.foxhole.gameskeeper.databinding.FragmentExploreBinding
import com.foxhole.gameskeeper.model.Game
import com.foxhole.gameskeeper.ui.MainActivity
import com.foxhole.gameskeeper.ui.MainViewModel
import com.foxhole.gameskeeper.ui.singleGame.SingleGameActivity
import com.foxhole.gameskeeper.utils.Constants.GAME_VIEW_TYPE
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.observeOn
import timber.log.Timber


class ExploreFragment : BaseFragment<FragmentExploreBinding>() {

    lateinit var viewModel: MainViewModel
    lateinit var gameAdapter: GameAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        gameAdapter = (requireActivity() as MainActivity).gameAdapter
        viewModel = (requireActivity() as MainActivity).mainViewModel

        intView()

        lifecycleScope.launchWhenCreated {
            viewModel.explore.collectLatest {
                gameAdapter.submitData(lifecycle, it)
            }
        }

        lifecycleScope.launchWhenStarted {
            gameAdapter.loadStateFlow.collectLatest { loadStates ->
                binding.swipeRefresh?.isRefreshing = loadStates.refresh is LoadState.Loading

                val isError = loadStates.refresh is LoadState.Error

                val snackbar = Snackbar.make(
                    requireView(),
                    "Network connection error",
                    Snackbar.LENGTH_INDEFINITE
                )
                snackbar.setAction("RETRY") {
                    gameAdapter.refresh()
                }

                if (isError) {
                    snackbar.show()
                } else {
                    snackbar.dismiss()
                }

            }
        }

        gameAdapter.onItemClick = { game ->
            Intent(requireActivity(), SingleGameActivity::class.java).also {
                it.putExtra("game", game)
                startActivity(it)
            }
        }

    }

    private fun intView() {
        val gridLayoutManager = GridLayoutManager(activity, 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = gameAdapter.getItemViewType(position)
                return if (viewType == GAME_VIEW_TYPE) 1
                else 2
            }
        }

        binding.exploreRecyclerView.apply {
            this.layoutManager = gridLayoutManager
            this.setHasFixedSize(true)
            this.adapter = gameAdapter.withLoadStateFooter(GameLoadStateAdapter(gameAdapter::retry))
        }
    }

    override fun setBinding(): FragmentExploreBinding =
        FragmentExploreBinding.inflate(layoutInflater)
}