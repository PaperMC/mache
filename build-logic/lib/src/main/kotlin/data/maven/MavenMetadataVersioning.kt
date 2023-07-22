package io.papermc.mache.lib.data.maven

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlChildrenName
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("versioning")
data class MavenMetadataVersioning(
    @XmlChildrenName("version")
    val versions: List<String>,
)
