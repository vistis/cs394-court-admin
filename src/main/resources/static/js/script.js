function togglePassword() {
  const input = document.getElementById("password");
  input.type = input.type === "password" ? "text" : "password";
}

function openDeleteModal(button) {
  const userId = button.dataset.userId;
  const userName = button.dataset.userName;
  document.getElementById("deleteUserName").textContent = userName;
  document.getElementById("deleteUserForm").action = "/admin/users/delete/" + userId;
  const modal = document.getElementById("deleteModal");
  modal.classList.remove("hidden");
  modal.classList.add("flex");
}

function closeDeleteModal() {
  const modal = document.getElementById("deleteModal");
  modal.classList.add("hidden");
  modal.classList.remove("flex");
}

document.addEventListener("DOMContentLoaded", function() {
  const modal = document.getElementById("deleteModal");
  if (modal) {
    modal.addEventListener("click", function (e) {
      if (e.target === this) closeDeleteModal();
    });
  }
});

function previewProfilePicture(input) {
  const textSpan = document.getElementById("file-name-text");
  const container = document.getElementById("image-container");
  if (input.files && input.files[0]) {
    const file = input.files[0];
    textSpan.textContent = file.name;
    textSpan.className = "text-xs text-emerald-600 font-medium block mt-1";
    const reader = new FileReader();
    reader.onload = function (e) {
      container.innerHTML = `<img src="${e.target.result}" alt="Profile Preview" class="h-full w-full object-cover" />`;
    };
    reader.readAsDataURL(file);
  }
}
