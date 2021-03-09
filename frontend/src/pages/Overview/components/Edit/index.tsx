import React, { useState } from "react";
import { Input, InputError, InputLabel } from "components/Input/styled";
import { GeneralError } from "components/Error/styled";
import Modal from "components/Modal";
import {
  ButtonGroup,
  CancelButton,
  EditButton,
  Form,
  InputContainer,
  Body,
} from "./styled";
import { Service } from "types/service";
import TEXTS from "texts";

const {
  editModal: { titleLabel, descriptionLabel },
  general: {
    nameLabel,
    urlLabel,
    savingLabel,
    saveLabel,
    cancelLabel,
    missingUrlErrorLabel,
    invalidUrlErrorLabel,
  },
} = TEXTS;

interface Props {
  name?: string;
  url?: string;
  savingError?: string;
  edit: (service?: Partial<Service>) => void;
}

/**
 * Edit service name and/or url
 * @param name - name of service
 * @param url - url of service
 * @param savingError - possible error when trying to update service
 */
export default function Edit({ name, url, savingError, edit }: Props) {
  const [newName, setNewName] = useState<string | undefined>(name);
  const [newUrl, setNewUrl] = useState<string | undefined>(url);
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState<boolean>(false);

  // Validate input and invoke callback to update service
  const save = () => {
    setSaving(true);
    if (!newUrl || newUrl.length === 0) {
      setError(missingUrlErrorLabel);
      setSaving(false);
      return;
    }
    // Validate url
    try {
      new URL(newUrl);
    } catch (err) {
      setError(invalidUrlErrorLabel);
      setSaving(false);
      return;
    }
    edit({ Name: newName, Url: newUrl });
  };

  return (
    <Modal title={titleLabel} description={descriptionLabel}>
      <Body>
        <Form>
          <InputContainer>
            <InputLabel>{nameLabel}</InputLabel>
            <Input
              onChange={(event) => {
                setNewName(event.target.value);
              }}
              value={newName}
            />
          </InputContainer>
          <InputContainer>
            <InputLabel>{urlLabel}</InputLabel>
            <Input
              onChange={(event) => {
                setNewUrl(event.target.value);
              }}
              value={newUrl}
            />
            {error && <InputError>{error}</InputError>}
          </InputContainer>
        </Form>
        {savingError && <GeneralError>{savingError}</GeneralError>}
        <ButtonGroup>
          <EditButton disabled={saving} onClick={() => save()}>
            {saving ? savingLabel : saveLabel}
          </EditButton>
          <CancelButton onClick={() => edit()}>{cancelLabel}</CancelButton>
        </ButtonGroup>
      </Body>
    </Modal>
  );
}
