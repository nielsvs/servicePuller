// Text used in application
const texts = {
  overview: {
    titleLabel: "Services",
    descriptionLabel:
      "Please find the status of the services below. You can also edit, update, add or remove services",
    editErrorLabel: "Service could not be edited. Try again or contact support",
    addServiceLabel: "Add service",
    addErrorLabel: "Service could not be added. Try again or contact support.",
    loadingErrorLabel:
      "Oops! Seems like an error occured 😅 try to reload the page or contact support.",
    noServicesLabel:
      "You don't seem to have any services yet. Start by adding a service above 😃",
  },
  addModal: {
    titleLabel: "Add service",
    descriptionLabel: "Below you can add a new service",
    
    urlInputPlaceholderLabel: "Enter the url of the service",
    nameInputPlaceholderLabel: "Enter the name of the service"
  },
  confirmModal: {
      titleLabel: "Delete service",
      descriptionLabel: (service: string) =>  `Are you sure you want to delete the service ${service}?`
  },
  editModal: {
    titleLabel: "Edit service",
    descriptionLabel: "Below you can edit the service url and name"
  },
  general: {
    urlLabel: "Url",
    nameLabel: "Name",
    statusLabel: "Status",
    updatedLabel: "Updated",
    actionLabel: "Action",
    saveLabel: "Save",
    savingLabel: "Saving...",
    loadingLabel: "Loading...",
    cancelLabel: "Cancel",
    deleteLabel: "Delete",
    deletingLabel: "Deleting...",
    missingUrlErrorLabel: "Please enter a url",
    invalidUrlErrorLabel: "Invalid url. Please check spelling",
  },
};

export default texts;
