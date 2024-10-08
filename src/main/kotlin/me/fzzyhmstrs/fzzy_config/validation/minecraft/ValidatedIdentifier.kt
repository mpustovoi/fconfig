/*
* Copyright (c) 2024 Fzzyhmstrs
*
* This file is part of Fzzy Config, a mod made for minecraft; as such it falls under the license of Fzzy Config.
*
* Fzzy Config is free software provided under the terms of the Timefall Development License - Modified (TDL-M).
* You should have received a copy of the TDL-M with this software.
* If you did not, see <https://github.com/fzzyhmstrs/Timefall-Development-Licence-Modified>.
* */

package me.fzzyhmstrs.fzzy_config.validation.minecraft

import com.mojang.brigadier.suggestion.Suggestions
import me.fzzyhmstrs.fzzy_config.entry.EntryValidator
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindow
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowListener
import me.fzzyhmstrs.fzzy_config.screen.internal.SuggestionWindowProvider
import me.fzzyhmstrs.fzzy_config.screen.widget.OnClickTextFieldWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.PopupWidget
import me.fzzyhmstrs.fzzy_config.screen.widget.TextureIds
import me.fzzyhmstrs.fzzy_config.updates.Updatable
import me.fzzyhmstrs.fzzy_config.util.AllowableIdentifiers
import me.fzzyhmstrs.fzzy_config.util.FcText
import me.fzzyhmstrs.fzzy_config.util.Translatable
import me.fzzyhmstrs.fzzy_config.util.RenderUtil.drawGuiTexture
import me.fzzyhmstrs.fzzy_config.util.ValidationResult
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofList
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofRegistry
import me.fzzyhmstrs.fzzy_config.validation.minecraft.ValidatedIdentifier.Companion.ofTag
import me.fzzyhmstrs.fzzy_config.validation.misc.ChoiceValidator
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.ClickableWidget
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Formatting
import net.minecraft.util.Identifier
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import org.jetbrains.annotations.ApiStatus.Internal
import org.lwjgl.glfw.GLFW
import java.util.concurrent.CompletableFuture
import java.util.function.*

/**
 * A validated Identifier field.
 *
 * There are various shortcut methods available for building ValidatedIdentifiers more easily than with the primary constructor. Check out options in the See Also section
 * @param defaultValue String, the string value of the default identifier
 * @param allowableIds [AllowableIdentifiers] instance. Defines the predicate for valid ids, and the supplier of valid id lists
 * @param validator [EntryValidator]<String> handles validation of individual entries. Defaults to validation based on the predicate provided in allowableIds
 * @sample me.fzzyhmstrs.fzzy_config.examples.ValidatedMiscExamples.identifiers
 * @see ofTag
 * @see ofRegistry
 * @see ofList

 * @author fzzyhmstrs
 * @since 0.1.2
 */
@Suppress("unused")
open class ValidatedIdentifier @JvmOverloads constructor(defaultValue: Identifier, val allowableIds: AllowableIdentifiers, private val validator: EntryValidator<Identifier> = allowableIds)
    :
    ValidatedField<Identifier>(defaultValue),
    Updatable,
    Translatable,
    Comparable<Identifier>
{
    /**
     * An unbounded validated identifier
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [Identifier] the default identifier for this validation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: Identifier): this(defaultValue, AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from a string
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultValue [String] the default identifier (in string form) for this validation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultValue: String): this(Identifier.of(defaultValue), AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier constructed from namespace and path strings
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @param defaultNamespace [String] the default namespace for this validation
     * @param defaultPath [String] the default path for this validation
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(defaultNamespace: String, defaultPath: String): this(Identifier.of(defaultNamespace, defaultPath), AllowableIdentifiers.ANY)

    /**
     * An unbounded validated identifier with a dummy default value
     *
     * Validation will be limited to ensuring inputs are valid identifiers
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    constructor(): this(Identifier.of("c:/c"), AllowableIdentifiers.ANY)

    /**
     * Creates a deep copy of the stored value and returns it
     * @return Identifier - deep copy of the currently stored value
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun copyStoredValue(): Identifier {
        return Identifier.of(storedValue.toString())
    }
    @Internal
    override fun deserialize(toml: TomlElement, fieldName: String): ValidationResult<Identifier> {
        return try {
            val string = toml.toString()
            val id = Identifier.tryParse(string) ?: return ValidationResult.error(storedValue, "Invalid identifier [$fieldName].")
            ValidationResult.success(id)
        } catch (e: Exception) {
            ValidationResult.error(storedValue, "Critical error deserializing identifier [$fieldName]: ${e.localizedMessage}")
        }
    }
    @Internal
    override fun serialize(input: Identifier): ValidationResult<TomlElement> {
        return ValidationResult.success(TomlLiteral(input.toString()))
    }
    @Internal
    override fun correctEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        val result = validator.validateEntry(input, type)
        return if(result.isError()) {
            ValidationResult.error(storedValue, "Invalid identifier [$input] found, corrected to [$storedValue]: ${result.getError()}")} else result
    }
    @Internal
    override fun validateEntry(input: Identifier, type: EntryValidator.ValidationType): ValidationResult<Identifier> {
        return validator.validateEntry(input, type)
    }

    /**
     * creates a deep copy of this ValidatedIdentifier
     * @return ValidatedIdentifier wrapping a deep copy of the currently stored identifier, as well as this validations validator
     * @author fzzyhmstrs
     * @since 0.2.0
     */
    override fun instanceEntry(): ValidatedIdentifier {
        return ValidatedIdentifier(copyStoredValue(), allowableIds, validator)
    }
    @Internal
    override fun isValidEntry(input: Any?): Boolean {
        return input is Identifier && validateEntry(input, EntryValidator.ValidationType.STRONG).isValid()
    }
    @Internal
    @Environment(EnvType.CLIENT)
    override fun widgetEntry(choicePredicate: ChoiceValidator<Identifier>): ClickableWidget {
        return OnClickTextFieldWidget({ this.get().toString() }, { it, isKb, key, code, mods ->
            val textField = PopupIdentifierTextFieldWidget(170, 20, choicePredicate, this)
            val popup = PopupWidget.Builder(this.translation())
                .addElement("text_field", textField, PopupWidget.Builder.Position.BELOW)
                .addDoneButton({ textField.pushChanges(); PopupWidget.pop() })
                .positionX { _, _ -> it.x - 8 }
                .positionY { _, h -> it.y + 28 + 24 - h }
                .build()
            PopupWidget.push(popup)
            PopupWidget.focusElement(textField)
            if (isKb)
                textField.keyPressed(key, code, mods)
        })
    }

    ////////////////////////

    /**
     * @return the path of the cached Identifier
     */
    fun getPath(): String {
        return storedValue.path
    }
    /**
     * @return the namespace of the cached Identifier
     */
    fun getNamespace(): String {
        return storedValue.namespace
    }

    fun withPath(path: String?): Identifier {
        return storedValue.withPath(path)
    }

    fun withPath(pathFunction: UnaryOperator<String?>): Identifier {
        return storedValue.withPath(pathFunction)
    }

    fun withPrefixedPath(prefix: String): Identifier {
        return storedValue.withPrefixedPath(prefix)
    }

    fun withSuffixedPath(suffix: String): Identifier {
        return storedValue.withSuffixedPath(suffix)
    }

    /**
     * @suppress
     */
    override fun toString(): String {
        return storedValue.toString()
    }

    override fun translationKey(): String {
        @Suppress("DEPRECATION")
        return getEntryKey()
    }

    override fun descriptionKey(): String {
        @Suppress("DEPRECATION")
        return getEntryKey() + ".desc"
    }

    override fun equals(other: Any?): Boolean {
        return storedValue == other
    }

    override fun hashCode(): Int {
        return storedValue.hashCode()
    }

    override fun compareTo(other: Identifier): Int {
        return storedValue.compareTo(other)
    }

    fun toUnderscoreSeparatedString(): String {
        return storedValue.toUnderscoreSeparatedString()
    }

    fun toTranslationKey(): String {
        return storedValue.toTranslationKey()
    }

    fun toShortTranslationKey(): String {
        return storedValue.toShortTranslationKey()
    }

    fun toTranslationKey(prefix: String): String {
        return storedValue.toTranslationKey(prefix)
    }

    fun toTranslationKey(prefix: String, suffix: String): String? {
        return storedValue.toTranslationKey(prefix, suffix)
    }


    companion object {
        @JvmStatic
        val DEFAULT_WEAK: EntryValidator<Identifier> = EntryValidator { i, _ -> ValidationResult.success(i) }

        /**
         * builds a String EntryValidator with default behavior
         *
         * Use if your identifier list may not be available at load-time (during modInitialization, typically), but will be available during updating (in-game). Lists from a Tag or Registry are easy examples, as the registry may not be fully populated yet, and the tag may not be loaded.
         * @param allowableIds an [AllowableIdentifiers] instance.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun default(allowableIds: AllowableIdentifiers): EntryValidator<Identifier> {
            return EntryValidator.Builder<Identifier>()
                .weak(DEFAULT_WEAK)
                .strong { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .buildValidator()
        }

        /**
         * builds a String EntryValidator with always-strong behavior
         *
         * Use if your identifier list is available both at loading (during modInitialization, typically), and during updating (in-game).
         * @param allowableIds an [AllowableIdentifiers] instance.
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun strong(allowableIds: AllowableIdentifiers): EntryValidator<Identifier> {
            return EntryValidator.Builder<Identifier>()
                .weak { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .strong { i, _ -> ValidationResult.predicated(i, allowableIds.test(i), "Identifier invalid or not allowed") }
                .buildValidator()
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided default and tag
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <T> ofTag(defaultValue: Identifier, tag: TagKey<T>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ false }, { listOf() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable tag of values
         *
         * Uses "minecraft:air" as the default value.
         * @param tag the tag of allowable values to choose from
         * @return [ValidatedIdentifier] wrapping the provided tag
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofTag(tag: TagKey<T>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(tag.registry().value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val supplier = Supplier { registry.iterateEntries(tag).mapNotNull { registry.getId(it.value()) } }
            return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier))
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue, AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate Predicate<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided default and predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun <T> ofRegistry(defaultValue: Identifier, registry: Registry<T>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(defaultValue,
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test ((registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } }
                )
            )
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values
         *
         * Uses "minecraft:air" as the default value
         * @param registry the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>): ValidatedIdentifier {
            return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ id -> registry.containsId(id) }, { registry.ids.toList() }))
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, filtered by the provided predicate
         *
         * Uses "minecraft:air" as the default value
         * @param registry the registry whose ids are valid for this identifier
         * @param predicate [BiPredicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided predicated registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistry(registry: Registry<T>, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            return ValidatedIdentifier(Identifier.of("minecraft:air"),
                AllowableIdentifiers(
                    { id -> registry.containsId(id) && predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@AllowableIdentifiers false).get()) },
                    { registry.ids.filter { id -> predicate.test (id, (registry.getEntry(id).takeIf { it.isPresent } ?: return@filter false).get()) } }
                )
            )
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Uses "minecraft:air" as the default value
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistryKey(defaultValue: Identifier, key: RegistryKey<Registry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            return ofRegistry(defaultValue, registry)
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistryKey(defaultValue: Identifier, key: RegistryKey<Registry<T>>, predicate: Predicate<RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            return ofRegistry(defaultValue, registry, predicate)
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Uses "minecraft:air" as the default value
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistryKey(key: RegistryKey<Registry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            @Suppress("DEPRECATION")
            return ofRegistry(registry)
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable registry of values, defined from a RegistryKey
         *
         * Uses "minecraft:air" as the default value
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [BiPredicate]<RegistryEntry> tests an allowable subset of the registry
         * @return [ValidatedIdentifier] wrapping the provided registry
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        @Deprecated("Only use for validation in a list or map")
        fun <T> ofRegistryKey(key: RegistryKey<Registry<T>>, predicate: BiPredicate<Identifier, RegistryEntry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val registry = maybeRegistry.get() as? Registry<T> ?: return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            @Suppress("DEPRECATION")
            return ofRegistry(registry, predicate)
        }

        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey
         *
         * Uses "c:dummy" as the default TagKey id
         * @param T the TagKey type
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         */
        fun <T: Any> ofRegistryTags(key: RegistryKey<out Registry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry.get().streamTags().map { it.tag.id }.toList() }
            val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier)
            return ValidatedIdentifier(Identifier.of("c:dummy"), ids)
        }
        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey, and predicated by the provided predicate
         *
         * Uses "c:dummy" as the default TagKey id
         * @param T the TagKey type
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<Identifier> tests an allowable subset of the TagKeys
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         */
        fun <T: Any> ofRegistryTags(key: RegistryKey<out Registry<T>>, predicate: Predicate<Identifier>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry.get().streamTags().map { it.tag.id }.filter(predicate).toList() }
            val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier)
            return ValidatedIdentifier(Identifier.of("c:dummy"), ids)
        }
        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey, and predicated by the provided predicate
         * @param T the TagKey type
         * @param default [TagKey] the default TagKey value to get an identifier from
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @param predicate [Predicate]<Identifier> tests an allowable subset of the TagKeys
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         */
        fun <T: Any> ofRegistryTags(default: TagKey<T>, key: RegistryKey<out Registry<T>>, predicate: Predicate<Identifier>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry.get().streamTags().map { it.tag.id }.filter(predicate).toList() }
            val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier)
            return ValidatedIdentifier(default.id, ids)
        }
        /**
         * Builds a ValidatedIdentifier based on the existing [TagKey] stream from the registry defined by the supplied RegistryKey
         * @param T the TagKey type
         * @param default [TagKey] the default TagKey value to get an identifier from
         * @param key [RegistryKey] for the registry whose ids are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the TagKeys of the provided registry
         */
        fun <T: Any> ofRegistryTags(default: TagKey<T>, key: RegistryKey<out Registry<T>>): ValidatedIdentifier {
            val maybeRegistry = Registries.REGISTRIES.getOrEmpty(key.value)
            if (maybeRegistry.isEmpty) return ValidatedIdentifier(Identifier.of("minecraft:air"), AllowableIdentifiers({ false }, { listOf() }))
            val supplier: Supplier<List<Identifier>> = Supplier { maybeRegistry.get().streamTags().map { it.tag.id }.toList() }
            val ids = AllowableIdentifiers({ id -> supplier.get().contains(id) }, supplier)
            return ValidatedIdentifier(default.id, ids)
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list should be available and complete at validation time
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param list the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun ofList(defaultValue: Identifier, list: List<Identifier>): ValidatedIdentifier {
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier(defaultValue, allowableIds, validator)
        }

        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list should be available and complete at validation time
         *
         * uses "minecraft:air" as the default value
         * @param list the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided list
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        @Deprecated("Use only for validation of a list or map. Make sure your list is available at Validation time! (Typically at ModInitializer call or earlier)")
        fun ofList(list: List<Identifier>): ValidatedIdentifier {
            val allowableIds = AllowableIdentifiers({ id -> list.contains(id) }, list.supply())
            val validator = strong(allowableIds)
            return ValidatedIdentifier(Identifier.of("minecraft:air"), allowableIds, validator)
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list does not have to be complete at validation time.
         * @param defaultValue the default value of the ValidatedIdentifier
         * @param listSupplier Supplier of the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided default and list supplier
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofSuppliedList(defaultValue: Identifier, listSupplier: Supplier<List<Identifier>>): ValidatedIdentifier {
            val allowableIds = AllowableIdentifiers({ id -> listSupplier.get().contains(id) }, listSupplier)
            return ValidatedIdentifier(defaultValue, allowableIds)
        }
        /**
         * Builds a ValidatedIdentifier based on an allowable list of values
         *
         * This list does not have to be complete at validation time.
         *
         * uses "minecraft:air" as the default value
         * @param listSupplier Supplier of the list whose entries are valid for this identifier
         * @return [ValidatedIdentifier] wrapping the provided list supplier
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @JvmStatic
        fun ofSuppliedList(listSupplier: Supplier<List<Identifier>>): ValidatedIdentifier {
            val allowableIds = AllowableIdentifiers({ id -> listSupplier.get().contains(id) }, listSupplier)
            return ValidatedIdentifier(Identifier.of("minecraft:air"), allowableIds)
        }

        /**
         * wraps a list in a [Supplier]
         * @author fzzyhmstrs
         * @since 0.2.0
         */
        @Suppress("MemberVisibilityCanBePrivate")
        fun<T> List<T>.supply(): Supplier<List<T>> {
            return Supplier { this }
        }
    }

    @Environment(EnvType.CLIENT)
    private class PopupIdentifierTextFieldWidget(
        width: Int,
        height: Int,
        private val choiceValidator: ChoiceValidator<Identifier>,
        private val validatedIdentifier: ValidatedIdentifier)
        :
        TextFieldWidget(MinecraftClient.getInstance().textRenderer, 0, 0, width, height, FcText.empty()),
        SuggestionWindowProvider
    {

        private var cachedWrappedValue = validatedIdentifier.get()
        private var storedValue = validatedIdentifier.get()
        private var lastChangedTime: Long = 0L
        private var isValid = true
        private var pendingSuggestions: CompletableFuture<Suggestions>? = null
        private var lastSuggestionText = ""
        private var shownText = ""
        private var window: SuggestionWindow? = null
        private var closeWindow = false
        private var needsUpdating = false
        private var suggestionWindowListener: SuggestionWindowListener? = null

        override fun addListener(listener: SuggestionWindowListener) {
            this.suggestionWindowListener = listener
        }

        private fun isValidTest(s: String): Boolean {
            if (s != lastSuggestionText) {
                pendingSuggestions = validatedIdentifier.allowableIds.getSuggestions(s, this.cursor, choiceValidator)
                lastSuggestionText = s
            }
            val id = Identifier.tryParse(s)
            if (id == null) {
                setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                return false
            }
            return if (validatedIdentifier.validateEntry(id, EntryValidator.ValidationType.STRONG).isValid()) {
                val result = choiceValidator.validateEntry(id, EntryValidator.ValidationType.STRONG)
                if (result.isValid()) {
                    storedValue = result.get()
                    lastChangedTime = System.currentTimeMillis()
                    setEditableColor(0xFFFFFF)
                    true
                } else {
                    setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                    false
                }
            } else {
                setEditableColor(Formatting.RED.colorValue ?: 0xFFFFFF)
                false
            }
        }

        override fun getInnerWidth(): Int {
            return super.getInnerWidth() - 11
        }

        private fun isChanged(): Boolean {
            return storedValue != validatedIdentifier.get()
        }

        private fun ongoingChanges(): Boolean {
            return System.currentTimeMillis() - lastChangedTime <= 350L
        }

        fun pushChanges() {
            if(isChanged() && !needsUpdating) {
                validatedIdentifier.accept(storedValue)
            }
        }

        override fun renderWidget(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
            val testValue = validatedIdentifier.get()
            if (cachedWrappedValue != testValue || needsUpdating) {
                needsUpdating = false
                this.storedValue = testValue
                this.cachedWrappedValue = testValue
                this.text = this.storedValue.toString()
            }
            if(isChanged()) {
                if (lastChangedTime != 0L && !ongoingChanges())
                    validatedIdentifier.accept(storedValue)
            }
            super.renderWidget(context, mouseX, mouseY, delta)
            if(isValid) {
                if (ongoingChanges())
                    context.drawGuiTexture(TextureIds.ENTRY_ONGOING, x + width - 20, y, 20, 20)
                else
                    context.drawGuiTexture(TextureIds.ENTRY_OK, x + width - 20, y, 20, 20)
            } else {
                context.drawGuiTexture(TextureIds.ENTRY_ERROR, x + width - 20, y, 20, 20)
            }
            if (pendingSuggestions?.isDone == true) {
                val suggestions = pendingSuggestions?.get()
                if (suggestions != null && !suggestions.isEmpty && shownText != lastSuggestionText) {
                    shownText = lastSuggestionText
                    addSuggestionWindow(suggestions)
                }
            }
            window?.render(context, mouseX, mouseY, delta)
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            val bl = window?.mouseClicked(mouseX.toInt(), mouseY.toInt(), button) ?: super.mouseClicked(mouseX, mouseY, button)
            if (closeWindow) {
                pendingSuggestions = null
                window = null
                suggestionWindowListener?.setSuggestionWindowElement(null)
                closeWindow = false
            }
            return if(bl) true else super.mouseClicked(mouseX, mouseY, button)
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
            return window?.mouseScrolled(mouseX.toInt(), mouseY.toInt(), verticalAmount) ?: super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
        }

        override fun isMouseOver(mouseX: Double, mouseY: Double): Boolean {
            return super.isMouseOver(mouseX, mouseY) || window?.isMouseOver(mouseX.toInt(), mouseY.toInt()) == true
        }

        override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
            val bl = window?.keyPressed(keyCode, scanCode, modifiers) ?: super.keyPressed(keyCode, scanCode, modifiers)
            if (closeWindow) {
                pendingSuggestions = null
                window = null
                suggestionWindowListener?.setSuggestionWindowElement(null)
                closeWindow = false
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                pushChanges()
                PopupWidget.pop()
            }
            return if(bl) true else super.keyPressed(keyCode, scanCode, modifiers)
        }

        init {
            setMaxLength(1000)
            text = validatedIdentifier.get().toString()
            setChangedListener { s -> isValid = isValidTest(s) }
        }

        private fun addSuggestionWindow(suggestions: Suggestions) {
            val applier: Consumer<String> = Consumer { s ->
                try {
                    validatedIdentifier.accept(Identifier.of(s))
                    needsUpdating = true
                } catch (e: Exception) {
                    //
                }
            }
            val closer: Consumer<SuggestionWindow> = Consumer { closeWindow = true }
            this.window = SuggestionWindow.createSuggestionWindow(this.x, this.y, suggestions, this.text, this.cursor, applier, closer)
            suggestionWindowListener?.setSuggestionWindowElement(this)
        }
    }
}