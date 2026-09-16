# tag::imports[]
from jakarta.persistence import Entity
from micronaut.core.annotation import Introspected
# end::imports[]


# tag::clazz[]
@Introspected(packages="micronaut.docs.hibernate.entityscan.external", includedAnnotations=[Entity])  # <1>
class Application:
    pass
# end::clazz[]
