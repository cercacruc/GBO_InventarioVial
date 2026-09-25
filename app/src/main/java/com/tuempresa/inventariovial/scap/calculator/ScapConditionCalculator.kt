package com.tuempresa.inventariovial.scap.calculator

import kotlin.math.abs
import kotlin.math.pow
import com.tuempresa.inventariovial.scap.data.ScapElementConditionEntity

object ScapPercentages {
    fun values(c:ScapElementConditionEntity)=listOf(c.percent0,c.percent1,c.percent2,c.percent3,c.percent4,c.percent5)
    fun error(percentages:List<Double?>):String? {
        if(percentages.size!=6 || percentages.any {it==null}) return "Completa los seis porcentajes, usando 0 cuando corresponda."
        val p=percentages.filterNotNull()
        if(p.any {!it.isFinite() || it<0 || it>100}) return "Cada porcentaje debe estar entre 0 y 100."
        if(abs(p.sum()-100)>0.0100000001) return "La suma debe ser 100% (tolerancia 0.01). Actual: ${p.sum()}%."
        return null
    }
}
data class ScapCalculationInput(val code:String,val importanceFactor:Double,val percentages:List<Double?>)
data class ScapElementResult(val code:String,val condition:Double,val contribution:Double,val adjusted:List<Double>)
data class ScapCalculationResult(val value:Double?,val classification:String?,val elements:List<ScapElementResult>,val reason:String?=null)

/** Literal implementation of sheet G; see docs/SCAP_CALCULATION.md, including its edge cases. */
object ScapConditionCalculator {
    fun calculate(inputs:List<ScapCalculationInput>):ScapCalculationResult {
        if(inputs.isEmpty()) return ScapCalculationResult(null,null,emptyList(),"Sin elementos evaluados.")
        val result=mutableListOf<ScapElementResult>()
        for(input in inputs) {
            ScapPercentages.error(input.percentages)?.let {return ScapCalculationResult(null,null,result,"${input.code}: $it")}
            if(!input.importanceFactor.isFinite() || input.importanceFactor !in 0.0..1.0)
                return ScapCalculationResult(null,null,result,"Factor de importancia inválido: ${input.code}.")
            val a=input.percentages.mapIndexed {i,p->p!!/(if(i==5) 3.0 else 25.0)*100}
            val c=DoubleArray(6)
            c[5]=a[5];c[4]=if(c[5]>100) 0.0 else c[5]+a[4]
            c[3]=if(c[4]>100) 0.0 else c[4]+a[3]
            c[2]=if(c[3]>=100) 0.0 else c[3]+a[2]
            c[1]=if(c[2]>=100 || c[3]>=100) 0.0 else c[2]+a[1]
            c[0]=if(c[1]>=100 || c[2]>=100 || c[3]>=100) 0.0 else c[1]+a[0]
            val adjusted=(0..5).map {i ->
                if(i==5) c[5] else {
                    val tail=(i..5).sumOf {c[it]}
                    maxOf(0.0,if(if(i==4) tail>100 else tail>=100) 100-(i+1..5).sumOf {c[it]} else c[i])
                }
            }
            if(adjusted.any {it !in 0.0..100.0} || abs(adjusted.sum()-100)>0.01)
                return ScapCalculationResult(null,null,result,"${input.code}: la fórmula de G produce una distribución fuera de 100%. Requiere revisión de ingeniería.")
            val condition=adjusted.mapIndexed {i,p->i.toDouble().pow(5)*p/100}.sum().pow(0.2)
            result+=ScapElementResult(input.code,condition,condition*input.importanceFactor,adjusted)
        }
        val max=result.maxOf {it.contribution}
        if(result.size<2 || max==0.0) return ScapCalculationResult(null,null,result,"La fórmula global divide entre cero con un único elemento o contribución máxima cero.")
        val value=max+(result.sumOf {it.contribution}-max)/(max*(result.size-1))
        if(!value.isFinite()) return ScapCalculationResult(null,null,result,"Resultado no finito; revisar la fórmula.")
        return ScapCalculationResult(value,when {value>4->"MUY MALO";value>3->"MALO";value>2->"REGULAR";else->"BUENO"},result)
    }
}
