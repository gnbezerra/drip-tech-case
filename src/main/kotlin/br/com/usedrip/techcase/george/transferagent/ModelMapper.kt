package br.com.usedrip.techcase.george.transferagent

import org.modelmapper.ModelMapper
import java.util.stream.Collectors
import java.util.stream.StreamSupport

/**
 * Extended ModelMapper with capabilities to map an Iterable, by mapping each element of the iterable separately and
 * sending the result to a List.
 */
class ModelMapper : ModelMapper() {
    fun <D> map(source: Iterable<Any>, destinationType: Class<D>): Iterable<D> {
        return StreamSupport.stream(source.spliterator(), false)
            .map { this.map(it, destinationType) }
            .collect(Collectors.toList())
    }
}
