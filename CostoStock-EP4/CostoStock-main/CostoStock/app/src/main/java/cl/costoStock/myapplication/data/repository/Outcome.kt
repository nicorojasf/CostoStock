package cl.costoStock.myapplication.data.repository

/** Resultado de una operación remota: el valor o un mensaje de error listo para mostrar. */
sealed interface Outcome<out T> {
    data class Ok<out T>(val value: T) : Outcome<T>
    data class Fail(val message: String) : Outcome<Nothing>
}
