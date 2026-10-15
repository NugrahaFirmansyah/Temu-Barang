package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val title: String) {
    INDONESIAN("id", "Bahasa Indonesia"),
    ENGLISH("en", "English")
}

object LanguageManager {
    var currentLanguage by mutableStateOf(AppLanguage.INDONESIAN)

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.INDONESIAN) AppLanguage.ENGLISH else AppLanguage.INDONESIAN
    }

    fun get(key: String): String {
        val map = if (currentLanguage == AppLanguage.INDONESIAN) idStrings else enStrings
        return map[key] ?: idStrings[key] ?: key
    }

    private val idStrings = mapOf(
        "app_title" to "TemuBarang",
        "app_subtitle" to "Temukan Barang Hilang Berbasis Lokasi & AI",
        "nav_home" to "Beranda",
        "nav_map" to "Peta",
        "nav_report" to "Lapor",
        "nav_matching" to "AI Match",
        "nav_chats" to "Pesan",
        "nav_profile" to "Profil",
        "nav_admin" to "Admin",
        "tab_lost" to "Kehilangan",
        "tab_found" to "Penemuan",
        "tab_all" to "Semua",
        "filter_category" to "Kategori",
        "search_hint" to "Cari nama barang, lokasi, atau merek...",
        "btn_report_lost" to "Lapor Kehilangan",
        "btn_report_found" to "Lapor Penemuan",
        "title_report_lost" to "Formulir Lapor Kehilangan",
        "title_report_found" to "Formulir Lapor Penemuan",
        "label_item_title" to "Nama / Judul Barang",
        "label_category" to "Kategori Barang",
        "label_desc" to "Deskripsi Ciri-Ciri Barang",
        "hint_desc" to "Warna, merek, stiker, nomor seri, goresan, dsb...",
        "label_location" to "Lokasi Perkiraan (Nama Tempat)",
        "label_date_time" to "Waktu Kejadian / Ditemukan",
        "label_photo" to "Foto Barang",
        "btn_choose_preset" to "Pilih Contoh Foto",
        "btn_submit_report" to "Kirim Laporan",
        "btn_saving" to "Menyimpan...",
        "msg_report_success" to "Laporan berhasil diterbitkan! AI sedang mencari kecocokan.",
        "status_open" to "Terbuka",
        "status_matched" to "Kecocokan Ditemukan",
        "status_resolved" to "Telah Ditemukan",
        "status_spam" to "Laporan Palsu/Spam",
        "badge_verified" to "Terverifikasi Admin",
        "ai_match_title" to "Kecocokan AI Cerdas",
        "ai_match_desc" to "Gemini AI membandingkan karakteristik visual dan deskripsi barang hilang & temuan.",
        "btn_run_ai_match" to "Jalankan Pencocokan AI",
        "ai_score" to "Skor Kecocokan",
        "btn_chat_safe" to "Mulai Chat Aman",
        "title_secure_chat" to "Chat Aman & Anonim",
        "chat_safe_notice" to "Nomor telepon & data pribadi Anda dirahasiakan.",
        "btn_send" to "Kirim",
        "btn_make_appointment" to "Ajukan Titik Temu Aman",
        "btn_mark_resolved" to "Konfirmasi Barang Kembali",
        "admin_dashboard" to "Panel Admin & Verifikasi",
        "admin_stat_total" to "Total Laporan",
        "admin_stat_verified" to "Terverifikasi",
        "admin_stat_spam" to "Spam/Palsu",
        "btn_verify_item" to "Verifikasi",
        "btn_flag_spam" to "Tandai Spam",
        "btn_delete" to "Hapus",
        "profile_title" to "Profil Pengguna",
        "my_reports" to "Riwayat Laporan Saya",
        "btn_sign_out" to "Keluar",
        "language_setting" to "Bahasa / Language",
        "empty_reports" to "Belum ada laporan barang.",
        "empty_matches" to "Belum ada kecocokan terdeteksi.",
        "empty_chats" to "Belum ada obrolan aktif.",
        "empty_notifications" to "Belum ada notifikasi baru.",
        "notifications_title" to "Pemberitahuan",
        "map_filter_all" to "Semua Pin",
        "map_filter_lost" to "Hanya Hilang",
        "map_filter_found" to "Hanya Ditemukan",
        "sign_in_title" to "Selamat Datang di TemuBarang",
        "sign_in_desc" to "Platform cerdas untuk menemukan kembali barang berharga Anda dengan aman dan terverifikasi.",
        "btn_sign_in_google" to "Masuk dengan Google"
    )

    private val enStrings = mapOf(
        "app_title" to "TemuBarang",
        "app_subtitle" to "Location & AI-Powered Lost & Found",
        "nav_home" to "Home",
        "nav_map" to "Map",
        "nav_report" to "Report",
        "nav_matching" to "AI Match",
        "nav_chats" to "Messages",
        "nav_profile" to "Profile",
        "nav_admin" to "Admin",
        "tab_lost" to "Lost",
        "tab_found" to "Found",
        "tab_all" to "All",
        "filter_category" to "Category",
        "search_hint" to "Search item name, place, brand...",
        "btn_report_lost" to "Report Lost Item",
        "btn_report_found" to "Report Found Item",
        "title_report_lost" to "Lost Item Form",
        "title_report_found" to "Found Item Form",
        "label_item_title" to "Item Title / Name",
        "label_category" to "Item Category",
        "label_desc" to "Detailed Characteristics",
        "hint_desc" to "Color, brand, unique stickers, serial number...",
        "label_location" to "Approximate Location",
        "label_date_time" to "Date & Time",
        "label_photo" to "Item Photo",
        "btn_choose_preset" to "Choose Preset Photo",
        "btn_submit_report" to "Submit Report",
        "btn_saving" to "Submitting...",
        "msg_report_success" to "Report published! AI is checking for matches.",
        "status_open" to "Open",
        "status_matched" to "Match Found",
        "status_resolved" to "Recovered",
        "status_spam" to "Flagged Fake/Spam",
        "badge_verified" to "Admin Verified",
        "ai_match_title" to "Smart AI Matching",
        "ai_match_desc" to "Gemini AI compares visual features and descriptions of lost & found items.",
        "btn_run_ai_match" to "Run AI Matching",
        "ai_score" to "Match Score",
        "btn_chat_safe" to "Start Secure Chat",
        "title_secure_chat" to "Secure & Anonymous Chat",
        "chat_safe_notice" to "Your phone number & private details stay protected.",
        "btn_send" to "Send",
        "btn_make_appointment" to "Safe Handover Point",
        "btn_mark_resolved" to "Confirm Item Returned",
        "admin_dashboard" to "Admin & Verification Panel",
        "admin_stat_total" to "Total Reports",
        "admin_stat_verified" to "Verified",
        "admin_stat_spam" to "Spam / Fake",
        "btn_verify_item" to "Verify",
        "btn_flag_spam" to "Flag as Spam",
        "btn_delete" to "Delete",
        "profile_title" to "User Profile",
        "my_reports" to "My Report History",
        "btn_sign_out" to "Sign Out",
        "language_setting" to "Language / Bahasa",
        "empty_reports" to "No item reports yet.",
        "empty_matches" to "No matches detected yet.",
        "empty_chats" to "No active conversations.",
        "empty_notifications" to "No new notifications.",
        "notifications_title" to "Notifications",
        "map_filter_all" to "All Pins",
        "map_filter_lost" to "Lost Only",
        "map_filter_found" to "Found Only",
        "sign_in_title" to "Welcome to TemuBarang",
        "sign_in_desc" to "Smart platform to safely recover your lost belongings with AI and verified community.",
        "btn_sign_in_google" to "Sign in with Google"
    )
}

fun appText(key: String): String = LanguageManager.get(key)
