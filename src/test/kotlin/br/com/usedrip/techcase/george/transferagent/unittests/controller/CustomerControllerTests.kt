package br.com.usedrip.techcase.george.transferagent.unittests.controller

import br.com.usedrip.techcase.george.transferagent.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.*
import org.mockito.kotlin.isA
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import uk.co.jemos.podam.api.PodamFactory

@SpringBootTest
@Import(TestConfig::class)
class CustomerControllerTests @Autowired constructor(val factory: PodamFactory) {
    @Mock
    private val customerRepository: CustomerRepository? = null

    @Spy
    private val modelMapper: ModelMapper? = null

    @InjectMocks
    private val customerController: CustomerController? = null

    @Captor
    private val customerCaptor: ArgumentCaptor<Customer>? = null

    @Nested
    inner class FindAll {
        private lateinit var customersResponse: Iterable<Customer>

        @BeforeEach
        fun setUp() {
            customersResponse = listOf(
                factory.manufacturePojoWithFullData(Customer::class.java),
                factory.manufacturePojo(Customer::class.java)
            )
            whenever(customerRepository?.findAll()).thenReturn(customersResponse)
        }

        @Test
        fun `findAll() returns list of customers`() {
            val expectedResponse = modelMapper?.map(customersResponse, CustomerDTO::class.java)

            val customerDTOs = customerController?.findAll()

            assertEquals(expectedResponse, customerDTOs)
            verify(customerRepository)?.findAll()
        }
    }

    @Nested
    inner class Save {
        private lateinit var customerDTORequest: CustomerDTO

        @BeforeEach
        fun setUp() {
            customerDTORequest = factory.manufacturePojo(CustomerDTO::class.java)
        }

        @Test
        fun `save() calls customer repository save operation and returns saved customer`() {
            val mockedCustomerResponse = factory.manufacturePojoWithFullData(Customer::class.java)
            whenever(customerRepository?.save(isA())).thenReturn(mockedCustomerResponse)
            val expectedCustomerDTOResponse = modelMapper?.map(mockedCustomerResponse, CustomerDTO::class.java)

            val response = customerController?.save(customerDTORequest)

            assertEquals(expectedCustomerDTOResponse, response)

            verify(customerRepository)?.save(customerCaptor!!.capture())
            val customerToSave = customerCaptor?.value
            assertEquals(customerDTORequest.fullName, customerToSave?.fullName)
            assertEquals(customerDTORequest.cpf, customerToSave?.cpf)
            assertEquals(customerDTORequest.id, customerToSave?.id)
            assertEquals(customerDTORequest.createdAt, customerToSave?.createdAt)
            assertEquals(customerDTORequest.updatedAt, customerToSave?.updatedAt)
        }

        @Test
        fun `throws CPFAlreadyExistsException when the repository already has a customer with the same cpf`() {
            whenever(customerRepository?.existsByCpf(isA())).thenReturn(true)

            assertThrows<CPFAlreadyExistsException> { customerController?.save(customerDTORequest) }
        }
    }
}
