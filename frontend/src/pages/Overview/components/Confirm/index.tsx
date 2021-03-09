import React, { useState } from "react";
import { GeneralError } from "components/Error/styled";
import Modal from "components/Modal";
import { ButtonGroup, CancelButton, DeleteButton } from "./styled";
import TEXTS from "texts";

const {
  confirmModal: { titleLabel, descriptionLabel },
  general: { deleteLabel, deletingLabel, cancelLabel },
} = TEXTS;

interface Props {
  name?: string;
  url?: string;
  savingError?: string;
  shouldDelete: (shouldDelete: boolean) => void;
}

/**
 * Delete service associated with name and/or url
 * @param name - name of service
 * @param url - url of service
 * @param savingError - possible error when trying to delete service
 * @param shouldDelete - callback to delete service
 */
export default function Confirm({
  name,
  url,
  savingError,
  shouldDelete,
}: Props) {
  // Either show name or url if possible
  const service = name ? `(${name})` : `(${url})` || "";
  const [saving, setSaving] = useState<boolean>(false);

  const handleClick = (should: boolean) => {
    setSaving(true);
    shouldDelete(should);
  };

  return (
    <Modal title={titleLabel} description={descriptionLabel(service)}>
      {savingError && <GeneralError>{savingError}</GeneralError>}
      <ButtonGroup>
        <DeleteButton disabled={saving} onClick={() => handleClick(true)}>
          {saving ? deletingLabel : deleteLabel}
        </DeleteButton>
        <CancelButton onClick={() => shouldDelete(false)}>
          {cancelLabel}
        </CancelButton>
      </ButtonGroup>
    </Modal>
  );
}
