import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { mapApiError } from '../api/errors'
import { profileApi } from '../api/profileApi'
import { ErrorState, LoadingState, useToast } from '../components/ui'
import {
  stationPreferenceSchema,
  type StationPreferenceFormValues,
} from '../forms/profileSchemas'

export function StationPreferencePage() {
  const queryClient = useQueryClient()
  const { notify } = useToast()
  const [error, setError] = useState<string | null>(null)
  const preferenceQuery = useQuery({
    queryKey: ['profile', 'station-preference'],
    queryFn: profileApi.stationPreference,
  })
  const form = useForm<StationPreferenceFormValues>({
    resolver: zodResolver(stationPreferenceSchema),
    defaultValues: {
      deskHeightCm: undefined,
      chairAngleDegree: undefined,
      rgbColor: '#00AEEF',
      brightness: 60,
      mouseDpi: 1600,
      nightMode: false,
    },
  })

  useEffect(() => {
    const preference = preferenceQuery.data
    if (preference) {
      form.reset({
        deskHeightCm: preference.deskHeightCm ?? undefined,
        chairAngleDegree: preference.chairAngleDegree ?? undefined,
        rgbColor: preference.rgbColor ?? '#00AEEF',
        brightness: preference.brightness ?? 60,
        mouseDpi: preference.mouseDpi ?? 1600,
        nightMode: preference.nightMode ?? false,
      })
    }
  }, [form, preferenceQuery.data])

  const mutation = useMutation({
    mutationFn: profileApi.updateStationPreference,
    onSuccess: () => {
      setError(null)
      notify('Station preference saved', 'success')
      void queryClient.invalidateQueries({ queryKey: ['profile', 'station-preference'] })
    },
    onError: (apiError) => setError(mapApiError(apiError)),
  })

  if (preferenceQuery.isLoading) {
    return <LoadingState label="Loading station preference" />
  }
  if (preferenceQuery.error) {
    return <ErrorState message={mapApiError(preferenceQuery.error)} />
  }

  return (
    <article className="panel narrow">
      <h2>Station preference</h2>
      <form className="form" onSubmit={form.handleSubmit((values) => mutation.mutate(clean(values)))}>
        {error ? <ErrorState message={error} /> : null}
        <label>
          Desk height
          <input
            type="number"
            min="60"
            max="120"
            {...form.register('deskHeightCm', { setValueAs: optionalNumberValue })}
          />
          <span>{form.formState.errors.deskHeightCm?.message}</span>
        </label>
        <label>
          Chair angle
          <input
            type="number"
            min="90"
            max="145"
            {...form.register('chairAngleDegree', { setValueAs: optionalNumberValue })}
          />
          <span>{form.formState.errors.chairAngleDegree?.message}</span>
        </label>
        <label>
          RGB color
          <input {...form.register('rgbColor')} />
          <span>{form.formState.errors.rgbColor?.message}</span>
        </label>
        <label>
          Brightness
          <input
            type="number"
            min="0"
            max="100"
            {...form.register('brightness', { setValueAs: optionalNumberValue })}
          />
          <span>{form.formState.errors.brightness?.message}</span>
        </label>
        <label>
          Mouse DPI
          <input
            type="number"
            min="200"
            max="32000"
            {...form.register('mouseDpi', { setValueAs: optionalNumberValue })}
          />
          <span>{form.formState.errors.mouseDpi?.message}</span>
        </label>
        <label className="inline-check">
          <input type="checkbox" {...form.register('nightMode')} />
          Night mode
        </label>
        <button className="button button-primary" type="submit" disabled={mutation.isPending}>
          Save preference
        </button>
      </form>
    </article>
  )
}

function optionalNumberValue(value: string) {
  return value === '' ? undefined : Number(value)
}

function clean(values: StationPreferenceFormValues) {
  return {
    ...values,
    rgbColor: values.rgbColor || undefined,
  }
}
