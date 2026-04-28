package mx.itson.edu.practica11

import android.os.Parcelable
import android.os.Parcel

data class Luchador(
    var id: Int = 0,
    var nombre: String? = null
) : Parcelable {
    override fun toString(): String = nombre ?: ""

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(nombre)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Luchador> {
        override fun createFromParcel(parcel: Parcel): Luchador {
            return Luchador(
                id = parcel.readInt(),
                nombre = parcel.readString()
            )
        }
        override fun newArray(size: Int): Array<Luchador?> = arrayOfNulls(size)
    }
}