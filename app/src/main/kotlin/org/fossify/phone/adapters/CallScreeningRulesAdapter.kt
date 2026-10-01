package org.fossify.phone.adapters

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.adapters.MyRecyclerViewAdapter
import org.fossify.commons.extensions.applyColorFilter
import org.fossify.commons.interfaces.ItemMoveCallback
import org.fossify.commons.interfaces.ItemTouchHelperContract
import org.fossify.commons.interfaces.StartReorderDragListener
import org.fossify.commons.views.MyRecyclerView
import org.fossify.phone.R
import org.fossify.phone.activities.SimpleActivity
import org.fossify.phone.databinding.ItemCallScreeningRuleBinding
import org.fossify.phone.helpers.ScreeningRule
import java.util.Collections

class CallScreeningRulesAdapter(
    activity: SimpleActivity,
    private val rules: MutableList<ScreeningRule>,
    recyclerView: MyRecyclerView,
    private val onRulesChanged: () -> Unit,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activity, recyclerView, itemClick), ItemTouchHelperContract {

    private val blockColor = activity.resources.getColor(R.color.md_red_700, activity.theme)
    private val allowColor = activity.resources.getColor(R.color.md_green_700, activity.theme)
    private val touchHelper = ItemTouchHelper(ItemMoveCallback(this, false))
    private val startReorderDragListener = object : StartReorderDragListener {
        override fun requestDrag(viewHolder: RecyclerView.ViewHolder) {
            touchHelper.startDrag(viewHolder)
        }
    }

    init {
        setupDragListener(true)
        touchHelper.attachToRecyclerView(recyclerView)
    }

    override fun getActionMenuId() = R.menu.cab_delete_only

    override fun prepareActionMode(menu: Menu) {}

    override fun actionItemPressed(id: Int) {
        if (selectedKeys.isEmpty()) {
            return
        }

        when (id) {
            R.id.cab_delete -> deleteSelectedRules()
        }
    }

    override fun getSelectableItemCount() = rules.size

    override fun getIsItemSelectable(position: Int) = true

    override fun getItemSelectionKey(position: Int) = rules.getOrNull(position)?.let { selectionKeyOf(it) }

    override fun getItemKeyPosition(key: Int) = rules.indexOfFirst { selectionKeyOf(it) == key }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeCreated() {
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActionModeDestroyed() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return createViewHolder(ItemCallScreeningRuleBinding.inflate(layoutInflater, parent, false).root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rule = rules[position]
        holder.bindView(rule, true, true) { itemView, layoutPosition ->
            setupView(ItemCallScreeningRuleBinding.bind(itemView), rule, holder)
        }
        bindViewHolder(holder)
    }

    override fun getItemCount() = rules.size

    /** Redraws the list after the activity added or edited a rule in the shared list. */
    @SuppressLint("NotifyDataSetChanged")
    fun refreshRules() {
        notifyDataSetChanged()
    }

    override fun onRowMoved(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (position in fromPosition until toPosition) {
                Collections.swap(rules, position, position + 1)
            }
        } else {
            for (position in fromPosition downTo toPosition + 1) {
                Collections.swap(rules, position, position - 1)
            }
        }

        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onRowSelected(myViewHolder: ViewHolder?) {}

    override fun onRowClear(myViewHolder: ViewHolder?) {
        onRulesChanged()
    }

    // Two rules can hold the very same action and pattern, so selection goes by object identity
    // rather than by value, to keep such twins apart in the action mode.
    private fun selectionKeyOf(rule: ScreeningRule) = System.identityHashCode(rule)

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteSelectedRules() {
        val selectedPositions = rules.indices.filter { selectedKeys.contains(selectionKeyOf(rules[it])) }
        finishActMode()
        selectedPositions.sortedDescending().forEach { rules.removeAt(it) }
        notifyDataSetChanged()
        onRulesChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView(binding: ItemCallScreeningRuleBinding, rule: ScreeningRule, holder: ViewHolder) {
        binding.apply {
            callScreeningRuleHolder.isSelected = selectedKeys.contains(selectionKeyOf(rule))

            callScreeningRuleLabel.apply {
                text = describeRule(rule)
                setTextColor(if (rule.block) blockColor else allowColor)
            }

            callScreeningRuleDragHandle.apply {
                applyColorFilter(textColor)
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        startReorderDragListener.requestDrag(holder)
                    }
                    false
                }
            }
        }
    }

    private fun describeRule(rule: ScreeningRule): String {
        val action = resources.getString(if (rule.block) R.string.call_screening_block else R.string.call_screening_allow)
        return "$action  ${rule.pattern}"
    }
}
