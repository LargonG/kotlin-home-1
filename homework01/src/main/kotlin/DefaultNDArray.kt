interface NDArray : SizeAware, DimensionAware {
    /*
     * Получаем значение по индексу point
     *
     * Если размерность point не равна размерности NDArray
     * бросаем IllegalPointDimensionException
     *
     * Если позиция по любой из размерностей некорректна с точки зрения
     * размерности NDArray, бросаем IllegalPointCoordinateException
     */
    fun at(point: Point): Int

    /*
     * Устанавливаем значение по индексу point
     *
     * Если размерность point не равна размерности NDArray
     * бросаем IllegalPointDimensionException
     *
     * Если позиция по любой из размерностей некорректна с точки зрения
     * размерности NDArray, бросаем IllegalPointCoordinateException
     */
    fun set(point: Point, value: Int)

    /*
     * Копируем текущий NDArray
     *
     */
    fun copy(): NDArray

    /*
     * Создаем view для текущего NDArray
     *
     * Ожидается, что будет создан новая реализация  интерфейса.
     * Но она не должна быть видна в коде, использующем эту библиотеку как внешний артефакт
     *
     * Должна быть возможность делать view над view.
     *
     * In-place-изменения над view любого порядка видна в оригнале и во всех view
     *
     * Проблемы thread-safety игнорируем
     */
    fun view(): NDArray

    /*
     * In-place сложение
     *
     * Размерность other либо идентична текущей, либо на 1 меньше
     * Если она на 1 меньше, то по всем позициям, кроме "лишней", она должна совпадать
     *
     * Если размерности совпадают, то делаем поэлементное сложение
     *
     * Если размерность other на 1 меньше, то для каждой позиции последней размерности мы
     * делаем поэлементное сложение
     *
     * Например, если размерность this - (10, 3), а размерность other - (10), то мы для три раза прибавим
     * other к каждому срезу последней размерности
     *
     * Аналогично, если размерность this - (10, 3, 5), а размерность other - (10, 5), то мы для пять раз прибавим
     * other к каждому срезу последней размерности
     */
    fun add(other: NDArray)

    /*
     * Умножение матриц. Immutable-операция. Возвращаем NDArray
     *
     * Требования к размерности - как для умножения матриц.
     *
     * this - обязательно двумерна
     *
     * other - может быть двумерной, с подходящей размерностью, равной 1 или просто вектором
     *
     * Возвращаем новую матрицу (NDArray размерности 2)
     *
     */
    fun dot(other: NDArray): NDArray
}

/*
 * Базовая реализация NDArray
 *
 * Конструкторы должны быть недоступны клиенту
 *
 * Инициализация - через factory-методы ones(shape: Shape), zeros(shape: Shape) и метод copy
 */
class DefaultNDArray private constructor(private val value: IntArray, private val shape: Shape) : NDArray {
    private fun getIndex(point: Point): Int {
        // Проверяем размерность
        if (point.ndim != shape.ndim) {
            throw NDArrayException.IllegalPointDimensionException(point.ndim, shape.ndim)
        }
        // Проверяем координаты
        for (i in 0 until shape.ndim) {
            if (point.dim(i) < 0 || shape.dim(i) <= point.dim(i)) {
                throw NDArrayException.IllegalPointCoordinateException(i, point.dim(i))
            }
        }

        var index = 0
        var coef = 1
        for (i in 0 until shape.ndim) {
            index += point.dim(i) * coef
            coef *= shape.dim(i)
        }

        return index
    }

    override fun at(point: Point): Int = value[getIndex(point)]

    override fun set(point: Point, value: Int) {
        this.value[getIndex(point)] = value
    }

    override fun copy(): NDArray = DefaultNDArray(value.clone(), shape)

    override fun view(): NDArray = this

    override fun add(other: NDArray) {
        if (other.ndim > ndim || other.ndim < ndim - 1 || !hasTheSameShapeWeak(other)) {
            throw Exception()
        }
        val iter = IntArray(other.ndim) { 0 }

        val shape = getShape(other)
        do {
            if (shape.ndim == ndim) {
                val point = DefaultPoint(*iter)
                set(point, at(point) + other.at(point))
            } else {
                for (i in 0 until dim(ndim - 1)) {
                    val point = DefaultPoint(*iter + IntArray(1) { i })
                    val pointOther = DefaultPoint(*iter)

                    set(point, at(point) + other.at(pointOther))
                }
            }
        } while (next(iter, shape))
    }

    override fun dot(other: NDArray): NDArray {
        val result = zeros(getShape(other));
        for (i in 0 until dim(0)) {
            for (j in 0 until dim(1)) {
                for (k in 0 until other.dim(0)) {
                    val pos = if (other.ndim == 2) DefaultPoint(i, j) else DefaultPoint(i)
                    val posA = DefaultPoint(i, k)
                    val posB = if (other.ndim == 2) DefaultPoint(k, j) else DefaultPoint(k)

                    result.set(pos, result.at(pos) + at(posA) * other.at(posB))
                }

                // делаем только один раз, если other - вектор
                if (other.ndim != 2) {
                    break
                }
            }
        }
        return result
    }

    private fun hasTheSameShapeWeak(other: NDArray): Boolean {
        for (i in 0 until Math.min(ndim, other.ndim)) {
            if (dim(i) != other.dim(i)) {
                return false;
            }
        }
        return true;
    }

    private fun hasTheSameShapeStrong(other: NDArray): Boolean {
        return ndim == other.ndim && hasTheSameShapeWeak(other)
    }

    override val size
        get() = shape.size
    override val ndim
        get() = shape.ndim

    override fun dim(i: Int) = shape.dim(i)

    companion object {
        fun ones(shape: Shape): NDArray = create(shape, 1)
        fun zeros(shape: Shape): NDArray = create(shape, 0)

        private fun create(shape: Shape, const: Int): NDArray {
            var size = 1
            for (i in 0 until shape.ndim) {
                size *= shape.dim(i)
            }
            val value = IntArray(size) { const }
            return DefaultNDArray(value, shape)
        }

        private fun next(point: IntArray, shape: Shape): Boolean {
            for (i in shape.ndim - 1 downTo 0) {
                if (point[i] < shape.dim(i) - 1) {
                    point[i]++
                    for (j in i + 1 until shape.ndim) {
                        point[j] = 0
                    }
                    return true
                }
            }
            return false
        }

        private fun getShape(array: NDArray): Shape {
            val result = IntArray(array.ndim) { 0 }
            for (i in 0 until array.ndim) {
                result[i] = array.dim(i)
            }
            return DefaultShape(*result)
        }
    }
}

sealed class NDArrayException(reason: String = "") : Exception(reason) {
    /* TODO: реализовать требуемые исключения */
    class IllegalPointCoordinateException(val index: Int, val value: Int, reason: String = "") :
        NDArrayException(reason) {

    }

    class IllegalPointDimensionException(val found: Int, val expected: Int, reason: String = "") :
        NDArrayException(reason) {

    }
}