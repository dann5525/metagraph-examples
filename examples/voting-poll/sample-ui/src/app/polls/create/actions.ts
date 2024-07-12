'use server';

import { revalidatePath, revalidateTag } from 'next/cache';
import { redirect } from 'next/navigation';
import { z } from 'zod';

import { PollSchema } from '../../../schemas';
import { MetagraphBaseURLs } from '../../../consts';

const CreatePollSchema = PollSchema.extend({
  signedPayload: z
    .string({ invalid_type_error: 'Invalid signed payload' })
    .regex(
      /^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/,
      'Invalid base64 character'
    )
});

type ICreatePollSchema = z.infer<typeof CreatePollSchema>;

export const createPoll = async (values: ICreatePollSchema) => {
  const validatedFields = CreatePollSchema.safeParse(values);

  if (!validatedFields.success) {
    return { errors: validatedFields.error.flatten().fieldErrors };
  }

  const decodedSignedPayload = JSON.parse(
    Buffer.from(validatedFields.data.signedPayload, 'base64').toString()
  );

  const response = await fetch(MetagraphBaseURLs.metagraphDataL1 + '/data', {
    method: 'post',
    body: decodedSignedPayload
  });

  const responseData = await response.json();

  console.log(responseData);

  revalidateTag('polls');
  revalidatePath('/polls');
  redirect(`/polls`);
};
