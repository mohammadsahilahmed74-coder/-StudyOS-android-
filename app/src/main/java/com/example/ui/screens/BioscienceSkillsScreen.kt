package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

data class BioscienceSkill(
    val id: String,
    val title: String,
    val category: String,
    val difficulty: String,
    val durationMin: Int,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val keyConcepts: List<String>,
    val practicalTasks: List<String>
)

/**
 * Bioscience Skills Learning & Lab Practice Hub
 * Covers core competencies in Molecular Biology, Genetics, Microbiology, Physiology, and Biotechnology.
 */
@Composable
fun BioscienceSkillsScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skills = remember {
        listOf(
            BioscienceSkill(
                id = "dna_extraction",
                title = "DNA & RNA Extraction",
                category = "Molecular Biology",
                difficulty = "Core Lab Skill",
                durationMin = 45,
                description = "Master cell lysis, protein precipitation, and nucleic acid purification protocols with ethanol precipitation.",
                icon = Icons.Default.Biotech,
                color = Color(0xFF6366F1),
                keyConcepts = listOf("Cell membrane lysis", "RNase/DNase inhibition", "Spectrophotometric quantification (A260/A280)"),
                practicalTasks = listOf("Prepare lysis buffer protocol", "Practice micropipetting precision", "Calculate nucleic acid yield and purity")
            ),
            BioscienceSkill(
                id = "pcr_gel_electrophoresis",
                title = "PCR & Gel Electrophoresis",
                category = "Genetics & Biotech",
                difficulty = "Essential Skill",
                durationMin = 60,
                description = "Design primers, configure thermal cycler denaturation/annealing/extension cycles, and run agarose gel analysis.",
                icon = Icons.Default.Science,
                color = Color(0xFF0EA5E9),
                keyConcepts = listOf("Taq polymerase kinetics", "Annealing temperature (Tm)", "Agarose concentration & DNA ladder sizing"),
                practicalTasks = listOf("Calculate primer annealing temps", "Prepare 1.5% agarose gel with fluorescent stain", "Interpret base-pair band size")
            ),
            BioscienceSkill(
                id = "aseptic_microbiology",
                title = "Aseptic Culture & Streaking",
                category = "Microbiology",
                difficulty = "Foundational",
                durationMin = 35,
                description = "Sterile technique, quadrant streak for single colonies, Gram staining identification, and colony counting.",
                icon = Icons.Default.Coronavirus,
                color = Color(0xFF10B981),
                keyConcepts = listOf("Quadrant streak technique", "Gram-positive vs Gram-negative cell walls", "Bacterial growth curves (OD600)"),
                practicalTasks = listOf("Perform Gram-stain smear and microscopy", "Plate serial dilutions for CFU/mL", "Sterilization by autoclaving")
            ),
            BioscienceSkill(
                id = "enzyme_kinetics",
                title = "Enzyme Kinetics & Assays",
                category = "Biochemistry",
                difficulty = "Advanced Skill",
                durationMin = 50,
                description = "Measure substrate affinity, maximum reaction velocity, and plot Lineweaver-Burk double reciprocal plots.",
                icon = Icons.Default.Spa,
                color = Color(0xFFF59E0B),
                keyConcepts = listOf("Michaelis-Menten constant (Km)", "Max velocity (Vmax)", "Competitive vs non-competitive inhibition"),
                practicalTasks = listOf("Plot spectrophotometric reaction rate", "Determine catalytic efficiency (kcat/Km)", "Analyze enzyme denaturation curve")
            ),
            BioscienceSkill(
                id = "cell_culture_viability",
                title = "Cell Culture & Hemocytometry",
                category = "Cell Biology",
                difficulty = "Hands-on Lab",
                durationMin = 40,
                description = "Maintain mammalian/yeast cell lines, perform trypan blue exclusion viability assays, and calculate seeding density.",
                icon = Icons.Default.Healing,
                color = Color(0xFFEC4899),
                keyConcepts = listOf("Hemocytometer grid calculations", "Trypan blue exclusion test", "Passaging split ratio & confluent density"),
                practicalTasks = listOf("Calculate total viable cells per mL", "Calculate media split volumes", "Examine cell morphology under phase contrast")
            ),
            BioscienceSkill(
                id = "bioinformatics_basics",
                title = "Bioinformatics & NCBI BLAST",
                category = "Computational Biology",
                difficulty = "Digital Skill",
                durationMin = 30,
                description = "Search genome databases, align nucleotide/protein sequences with BLAST, and model tertiary protein structures.",
                icon = Icons.Default.Psychology,
                color = Color(0xFF8B5CF6),
                keyConcepts = listOf("NCBI GenBank querying", "BLAST E-value statistical significance", "FASTA format sequence alignments"),
                practicalTasks = listOf("Run blastn alignment on mystery sequence", "Identify conserved functional domains", "Translate DNA codons into reading frames")
            )
        )
    }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All", "Molecular Biology", "Genetics & Biotech", "Microbiology", "Biochemistry", "Cell Biology", "Computational Biology") }

    val filteredSkills = remember(selectedCategory) {
        if (selectedCategory == "All") skills else skills.filter { it.category == selectedCategory }
    }

    var activeSkillForDetails by remember { mutableStateOf<BioscienceSkill?>(null) }
    var taskCreatedFeedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("bioscience_skills_screen_root")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("bioscience_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Bioscience Skills Lab",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Core lab competencies, protocols & practice tasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Notification / Task Added banner
        AnimatedVisibility(visible = taskCreatedFeedback != null) {
            taskCreatedFeedback?.let { feedback ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF059669))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feedback, style = MaterialTheme.typography.bodySmall, color = Color(0xFF065F46))
                    }
                }
            }
        }

        // Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { selectedCategory = category }
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredSkills, key = { it.id }) { skill ->
                BioscienceSkillCard(
                    skill = skill,
                    onClick = { activeSkillForDetails = skill },
                    onAddToTasks = {
                        viewModel.insertTask(
                            title = "Practice: ${skill.title}",
                            description = "Review concepts and complete practical task: ${skill.practicalTasks.firstOrNull() ?: skill.description}",
                            subjectId = null,
                            dueDateMillis = System.currentTimeMillis() + 86400000L,
                            priority = "HIGH",
                            estimatedMinutes = skill.durationMin
                        )
                        taskCreatedFeedback = "Added '${skill.title}' study task to your tasks!"
                    }
                )
            }
        }
    }

    // Detail Dialog for Skill
    activeSkillForDetails?.let { skill ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { activeSkillForDetails = null },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = skill.color.copy(alpha = 0.15f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(skill.icon, contentDescription = null, tint = skill.color, modifier = Modifier.size(28.dp))
                    }
                }
            },
            title = {
                Text(skill.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(skill.description, style = MaterialTheme.typography.bodyMedium)

                    Text("Key Theoretical Principles:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    skill.keyConcepts.forEach { concept ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("• ", color = skill.color, fontWeight = FontWeight.Bold)
                            Text(concept, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text("Practical Lab Competencies:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    skill.practicalTasks.forEach { task ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ ", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            Text(task, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.insertTask(
                            title = "Practice: ${skill.title}",
                            description = "Review concepts and complete practical task: ${skill.practicalTasks.firstOrNull() ?: skill.description}",
                            subjectId = null,
                            dueDateMillis = System.currentTimeMillis() + 86400000L,
                            priority = "HIGH",
                            estimatedMinutes = skill.durationMin
                        )
                        taskCreatedFeedback = "Added '${skill.title}' to your task schedule!"
                        activeSkillForDetails = null
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Study Tasks")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { activeSkillForDetails = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun BioscienceSkillCard(
    skill: BioscienceSkill,
    onClick: () -> Unit,
    onAddToTasks: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("skill_card_${skill.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = skill.color.copy(alpha = 0.15f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(skill.icon, contentDescription = null, tint = skill.color, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = skill.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = skill.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = skill.color,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${skill.durationMin}m",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = skill.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = skill.color.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = skill.difficulty,
                        style = MaterialTheme.typography.labelSmall,
                        color = skill.color,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                OutlinedButton(
                    onClick = onAddToTasks,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Tasks", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
