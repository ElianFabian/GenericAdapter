package com.elian.genericadapter.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KClass

open class GenericMultiItemAdapter<ItemT : Any>(
	areItemsTheSame: (oldItem: ItemT, newItem: ItemT) -> Boolean = { oldItem, newItem -> oldItem == newItem },
	areContentsTheSame: (oldItem: ItemT, newItem: ItemT) -> Boolean = { oldItem, newItem -> oldItem == newItem },
	itemBindings: List<BindingData<ItemT, *>>,
) : ListAdapter<ItemT, GenericMultiItemAdapter<ItemT>.ViewHolder>(
	object : DiffUtil.ItemCallback<ItemT>()
	{
		override fun areItemsTheSame(oldItem: ItemT, newItem: ItemT) = areItemsTheSame(oldItem, newItem)

		@SuppressLint("DiffUtilEquals")
		override fun areContentsTheSame(oldItem: ItemT, newItem: ItemT) = areContentsTheSame(oldItem, newItem)
	}
)
{
	inner class ViewHolder(val binding: ViewBinding, val bindingData: BindingData<ItemT, *>) : RecyclerView.ViewHolder(binding.root)


	private val bindings = itemBindings.distinct()
	private val itemClassToViewType = itemBindings.mapIndexed { index, data -> data.itemClass to index }.toMap()


	@Suppress("Unused")
	fun getItemAt(position: Int): ItemT? = getItem(position)


	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
	{
		val inflater = LayoutInflater.from(parent.context)

		val itemBindingData = bindings[viewType]

		val binding = itemBindingData.inflate(inflater, parent, false)

		return ViewHolder(binding, itemBindingData)
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int)
	{
		holder.bindingData.bindBlock(holder.binding, getItem(position), position, this)
	}

	override fun getItemViewType(position: Int): Int
	{
		val item = getItem(position)

		return itemClassToViewType[item::class]!!
	}
}

@Suppress("FunctionName")
fun <ItemT : Any> GenericAdapter(
	areItemsTheSame: (oldItem: ItemT, newItem: ItemT) -> Boolean = { oldItem, newItem -> oldItem == newItem },
	areContentsTheSame: (oldItem: ItemT, newItem: ItemT) -> Boolean = { oldItem, newItem -> oldItem == newItem },
	itemBindings: List<BindingData<ItemT, *>>,
): GenericMultiItemAdapter<ItemT>
{
	return GenericMultiItemAdapter(
		areItemsTheSame = areItemsTheSame,
		areContentsTheSame = areContentsTheSame,
		itemBindings = itemBindings,
	)
}

data class BindingData<out ItemT : Any, VB : ViewBinding>(
	val itemClass: KClass<@UnsafeVariance ItemT>,
	val inflate: (LayoutInflater, ViewGroup, Boolean) -> ViewBinding,
	val bindBlock: ViewBinding.(
		item: @UnsafeVariance ItemT,
		position: Int,
		adapter: GenericMultiItemAdapter<@UnsafeVariance ItemT>,
	) -> Unit,
)

@Suppress("FunctionName")
inline fun <reified ItemT : Any, VB : ViewBinding> Binding(
	noinline inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
	noinline bindBlock: VB.(item: ItemT) -> Unit,
): BindingData<ItemT, VB>
{
	return Binding(
		inflate = inflate,
		bindBlock = { item, _, _ ->

			bindBlock(this, item)
		}
	)
}

@Suppress("FunctionName", "UNCHECKED_CAST")
inline fun <reified ItemT : Any, VB : ViewBinding> Binding(
	noinline inflate: (LayoutInflater, ViewGroup, Boolean) -> VB,
	noinline bindBlock: VB.(
		item: ItemT,
		position: Int,
		adapter: GenericMultiItemAdapter<ItemT>,
	) -> Unit,
): BindingData<ItemT, VB>
{
	return BindingData(
		itemClass = ItemT::class,
		inflate = inflate,
		bindBlock = bindBlock as ViewBinding.(
			item: @UnsafeVariance ItemT,
			position: Int,
			adapter: GenericMultiItemAdapter<@UnsafeVariance ItemT>,
		) -> Unit,
	)
}