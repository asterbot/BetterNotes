package view

import model.ISubscriber
import model.Model

class ViewModel(private val model: Model): ISubscriber {
    init{
        model.subscribe(this)
    }

    override fun update(){
        println("Update called")
    }

}